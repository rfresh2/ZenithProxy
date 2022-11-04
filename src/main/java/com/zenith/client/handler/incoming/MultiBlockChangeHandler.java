package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class MultiBlockChangeHandler implements HandlerRegistry.AsyncIncomingHandler<ServerMultiBlockChangePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerMultiBlockChangePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().multiBlockUpdate(packet);
    }

    @Override
    public Class<ServerMultiBlockChangePacket> getPacketClass() {
        return ServerMultiBlockChangePacket.class;
    }
}
