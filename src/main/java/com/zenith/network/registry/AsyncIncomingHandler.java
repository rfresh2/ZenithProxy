package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

import static com.zenith.Shared.CLIENT_LOG;

public interface AsyncIncomingHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    /**
     * Call async (non-cancellable)
     * @param packet packet to handle
     * @param session Session the packet was received on
     */
    boolean applyAsync(P packet, S session);

    @Override
    default boolean apply(P packet, S session) {
        if (packet == null) return false;
        HandlerRegistry.ASYNC_EXECUTOR_SERVICE.submit(() -> {
            applyWithRetries(packet, session, 0);
        });
        return true;
    }

    private void applyWithRetries(P packet, S session, final int tryCount) {
        try {
            if (!applyAsync(packet, session)) {
                if (tryCount < 0 || tryCount > 1) {
                    CLIENT_LOG.warn("Unable to apply async handler for packet: " + packet.getClass().getSimpleName());
                    return;
                }
                HandlerRegistry.ASYNC_EXECUTOR_SERVICE.schedule(() -> {
                    applyWithRetries(packet, session, tryCount + 1);
                }, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("Async handler error", e);
        }
    }
}
