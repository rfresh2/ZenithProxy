package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;

import java.util.function.BiFunction;

public interface OutgoingHandler<P extends Packet, S extends Session> extends BiFunction<P, S, P> {
    P apply(P packet, S session);
}
