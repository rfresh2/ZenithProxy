package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

@FunctionalInterface
public interface PacketHandler<P extends Packet, S extends Session> {
    P apply(P packet, S session);
}
