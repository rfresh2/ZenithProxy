package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

import static com.zenith.Shared.CLIENT_LOG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@FunctionalInterface
public interface AsyncPacketHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    boolean applyAsync(P packet, S session);

    default P apply(P packet, S session) {
        if (packet == null) return null;
        HandlerRegistry.ASYNC_EXECUTOR_SERVICE.execute(() -> {
            applyWithRetries(packet, session, 0);
        });
        return packet;
    }

    private void applyWithRetries(P packet, S session, final int tryCount) {
        try {
            if (!applyAsync(packet, session)) {
                if (tryCount < 0 || tryCount > 1) {
                    CLIENT_LOG.warn("Unable to apply async handler for packet: " + packet.getClass().getSimpleName());
                    return;
                }
                HandlerRegistry.RETRY_EXECUTOR_SERVICE.schedule(() -> {
                    applyWithRetries(packet, session, tryCount + 1);
                }, 250, MILLISECONDS);
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("Async handler error", e);
        }
    }
}
