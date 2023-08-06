package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class BlockChangeHandler implements AsyncIncomingHandler<ServerBlockChangePacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ServerBlockChangePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().updateBlock(packet);
    }

    @Override
    public Class<ServerBlockChangePacket> getPacketClass() {
        return ServerBlockChangePacket.class;
    }
}
