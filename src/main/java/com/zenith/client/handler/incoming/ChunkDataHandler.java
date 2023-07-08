package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class ChunkDataHandler implements HandlerRegistry.AsyncIncomingHandler<ServerChunkDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerChunkDataPacket packet, @NonNull ClientSession session) {
        // todo: rework chunk data cache to not perform parsing until we have a block update in the chunk.
        //  essentially cache the raw packet data and only parse it when we need to.
        //  then handle gracefully in the cache whether we need to serialize a column or simply send a cached packet
        CACHE.getChunkCache().add(packet.readColumn(CACHE.getPlayerCache().getDimension() == 0));
        return true;
    }

    @Override
    public Class<ServerChunkDataPacket> getPacketClass() {
        return ServerChunkDataPacket.class;
    }
}
