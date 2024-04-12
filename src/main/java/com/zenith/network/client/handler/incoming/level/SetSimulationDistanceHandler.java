package com.zenith.network.client.handler.incoming.level;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetSimulationDistancePacket;

import static com.zenith.Shared.CACHE;

public class SetSimulationDistanceHandler implements ClientEventLoopPacketHandler<ClientboundSetSimulationDistancePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetSimulationDistancePacket packet, final ClientSession session) {
        CACHE.getChunkCache().setServerSimulationDistance(packet.getSimulationDistance());
        return true;
    }
}
