package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class MultiBlockChangeHandler implements AsyncIncomingHandler<ServerMultiBlockChangePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerMultiBlockChangePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().multiBlockUpdate(packet);
    }

    @Override
    public Class<ServerMultiBlockChangePacket> getPacketClass() {
        return ServerMultiBlockChangePacket.class;
    }
}
