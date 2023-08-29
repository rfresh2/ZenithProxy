package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class LevelChunkWithLightHandler implements AsyncIncomingHandler<ClientboundLevelChunkWithLightPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundLevelChunkWithLightPacket packet, @NonNull ClientSession session) {
        // todo: actually parse and cache
        CACHE.getChunkCache().add(packet);
//        CACHE.getChunkCache().add(parseColumn(packet));
        return true;
    }

    @Override
    public Class<ClientboundLevelChunkWithLightPacket> getPacketClass() {
        return ClientboundLevelChunkWithLightPacket.class;
    }
}
