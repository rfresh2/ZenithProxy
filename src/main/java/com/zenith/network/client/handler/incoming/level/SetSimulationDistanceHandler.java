package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetSimulationDistancePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class SetSimulationDistanceHandler implements AsyncIncomingHandler<ClientboundSetSimulationDistancePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetSimulationDistancePacket packet, final ClientSession session) {
        CACHE.getChunkCache().setServerSimulationDistance(packet.getSimulationDistance());
        return true;
    }

    @Override
    public Class<ClientboundSetSimulationDistancePacket> getPacketClass() {
        return ClientboundSetSimulationDistancePacket.class;
    }
}
