package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class LevelChunkWithLightHandler implements AsyncPacketHandler<ClientboundLevelChunkWithLightPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundLevelChunkWithLightPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().add(packet);
        return true;
    }
}
