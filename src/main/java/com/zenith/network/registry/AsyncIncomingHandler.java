package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.util.Wait;

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
        HandlerRegistry.ASYNC_EXECUTOR_SERVICE.submit(() -> {
            try {
                int iterCount = 0;
                while (!applyAsync(packet, session)) {
                    Wait.waitALittleMs(200);
                    if (iterCount++ > 3) {
                        CLIENT_LOG.warn("Unable to apply async handler for packet: " + packet.getClass().getSimpleName());
                        break;
                    }
                }
            } catch (final Throwable e) {
                CLIENT_LOG.error("Async handler error", e);
            }
        });
        return true;
    }
}
