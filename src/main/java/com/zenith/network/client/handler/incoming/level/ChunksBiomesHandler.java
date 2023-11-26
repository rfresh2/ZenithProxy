package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunksBiomesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class ChunksBiomesHandler implements AsyncPacketHandler<ClientboundChunksBiomesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundChunksBiomesPacket packet, final ClientSession session) {
        return CACHE.getChunkCache().handleChunkBiomes(packet);
    }
}
