package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class ChunkDataHandler implements HandlerRegistry.IncomingHandler<ServerChunkDataPacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ServerChunkDataPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().add(packet.getColumn());
        return true;
    }

    @Override
    public Class<ServerChunkDataPacket> getPacketClass() {
        return ServerChunkDataPacket.class;
    }
}
