package com.zenith.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import com.zenith.client.PorkClientSession;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;

/**
 * Xaero's WorldMap seems to care about this for some reason.
 */
public class SpawnPositionHandler implements HandlerRegistry.AsyncIncomingHandler<ServerSpawnPositionPacket, PorkClientSession> {
    @Override
    public boolean applyAsync(ServerSpawnPositionPacket packet, PorkClientSession session) {
        CACHE.getChunkCache().setSpawnPosition(packet.getPosition());
        return true;
    }

    @Override
    public Class<ServerSpawnPositionPacket> getPacketClass() {
        return ServerSpawnPositionPacket.class;
    }
}
