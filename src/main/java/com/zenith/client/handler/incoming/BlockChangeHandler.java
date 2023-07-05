package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class BlockChangeHandler implements HandlerRegistry.AsyncIncomingHandler<ServerBlockChangePacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ServerBlockChangePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().updateBlock(packet);
    }

    @Override
    public Class<ServerBlockChangePacket> getPacketClass() {
        return ServerBlockChangePacket.class;
    }
}
