package com.zenith.network.registry;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;

@FunctionalInterface
public interface PacketHandler<P extends Packet, S extends Session> {
    P apply(P packet, S session);
}
