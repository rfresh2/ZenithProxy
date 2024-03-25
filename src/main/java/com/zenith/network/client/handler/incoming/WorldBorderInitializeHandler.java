package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import com.zenith.cache.data.chunk.WorldBorderData;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;

public class WorldBorderInitializeHandler implements ClientEventLoopPacketHandler<ClientboundInitializeBorderPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundInitializeBorderPacket packet, final ClientSession session) {
        CACHE.getChunkCache().setWorldBorderData(new WorldBorderData(
                packet.getNewCenterX(),
                packet.getNewCenterZ(),
                packet.getNewSize(),
                packet.getNewAbsoluteMaxSize(),
                packet.getWarningBlocks(),
                packet.getWarningTime()
        ));
        return true;
    }
}
