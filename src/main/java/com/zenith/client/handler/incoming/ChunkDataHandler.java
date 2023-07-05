package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class ChunkDataHandler implements HandlerRegistry.AsyncIncomingHandler<ServerChunkDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerChunkDataPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().add(packet.getColumn());
        return true;
    }

    @Override
    public Class<ServerChunkDataPacket> getPacketClass() {
        return ServerChunkDataPacket.class;
    }
}
