package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

public interface PacketHandler<T extends Packet, S extends Session> {
    boolean apply(T packet, S session);
}
