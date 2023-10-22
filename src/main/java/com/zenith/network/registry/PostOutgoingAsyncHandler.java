package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

public interface PostOutgoingAsyncHandler<P extends Packet, S extends Session> extends PostOutgoingHandler<P, S> {

    void acceptAsync(P packet, S session);

    default void accept(P packet, S session) {
        if (packet == null) return;
        HandlerRegistry.ASYNC_EXECUTOR_SERVICE.execute(() -> {
            acceptAsync(packet, session);
        });
    }
}
