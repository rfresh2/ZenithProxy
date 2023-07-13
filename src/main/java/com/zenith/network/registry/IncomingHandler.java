package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

public interface IncomingHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> { }
