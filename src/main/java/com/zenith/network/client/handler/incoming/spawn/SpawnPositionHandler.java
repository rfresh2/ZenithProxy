package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

/**
 * Xaero's WorldMap seems to care about this for some reason.
 */
public class SpawnPositionHandler implements AsyncIncomingHandler<ServerSpawnPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerSpawnPositionPacket packet, ClientSession session) {
        CACHE.getChunkCache().setSpawnPosition(packet.getPosition());
        return true;
    }

    @Override
    public Class<ServerSpawnPositionPacket> getPacketClass() {
        return ServerSpawnPositionPacket.class;
    }
}
