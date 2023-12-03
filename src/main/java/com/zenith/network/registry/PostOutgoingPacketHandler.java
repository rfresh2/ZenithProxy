package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

@FunctionalInterface
public interface PostOutgoingPacketHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    @Override
    default P apply(P packet, S session) {
        accept(packet, session);
        return packet;
    }

    void accept(P packet, S session);
}
