package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class UnloadChunkHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUnloadChunkPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerUnloadChunkPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().remove(packet.getX(), packet.getZ());
        return true;
    }

    @Override
    public Class<ServerUnloadChunkPacket> getPacketClass() {
        return ServerUnloadChunkPacket.class;
    }
}
