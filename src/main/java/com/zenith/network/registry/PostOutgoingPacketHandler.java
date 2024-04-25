package com.zenith.network.registry;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;

@FunctionalInterface
public interface PostOutgoingPacketHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    @Override
    default P apply(P packet, S session) {
        accept(packet, session);
        return packet;
    }

    void accept(P packet, S session);
}
