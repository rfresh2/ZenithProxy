package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class SetChunkCacheCenterHandler implements AsyncPacketHandler<ClientboundSetChunkCacheCenterPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetChunkCacheCenterPacket packet, final ClientSession session) {
        CACHE.getChunkCache().setCenterX(packet.getChunkX());
        CACHE.getChunkCache().setCenterZ(packet.getChunkZ());
        return true;
    }
}
