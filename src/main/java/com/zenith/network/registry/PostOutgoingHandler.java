package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

import java.util.function.BiConsumer;

public interface PostOutgoingHandler<P extends Packet, S extends Session> extends BiConsumer<P, S> {
    void accept(P packet, S session);

    Class<P> getPacketClass();
}
