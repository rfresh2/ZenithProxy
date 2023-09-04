package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.util.Vec3i;

import static com.zenith.Shared.CACHE;

/**
 * Xaero's WorldMap seems to care about this for some reason.
 */
public class SpawnPositionHandler implements AsyncIncomingHandler<ClientboundSetDefaultSpawnPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSetDefaultSpawnPositionPacket packet, ClientSession session) {
        CACHE.getChunkCache().setSpawnPosition(Vec3i.from(packet.getPosition()));
        return true;
    }

    @Override
    public Class<ClientboundSetDefaultSpawnPositionPacket> getPacketClass() {
        return ClientboundSetDefaultSpawnPositionPacket.class;
    }
}
