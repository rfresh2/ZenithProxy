package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.util.math.MutableVec3i;

import static com.zenith.Shared.CACHE;

public class SpawnPositionHandler implements AsyncPacketHandler<ClientboundSetDefaultSpawnPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSetDefaultSpawnPositionPacket packet, ClientSession session) {
        CACHE.getPlayerCache().setSpawnPosition(MutableVec3i.from(packet.getPosition()));
        return true;
    }
}
