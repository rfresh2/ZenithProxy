package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.zenith.cache.data.entity.EntityExperienceOrb;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SpawnExperienceOrbHandler implements AsyncIncomingHandler<ServerSpawnExpOrbPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSpawnExpOrbPacket packet, @NonNull ClientSession session) {
        CACHE.getEntityCache().add(new EntityExperienceOrb()
                .setExp(packet.getExp())
                .setEntityId(packet.getEntityId())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
        );
        return true;
    }

    @Override
    public Class<ServerSpawnExpOrbPacket> getPacketClass() {
        return ServerSpawnExpOrbPacket.class;
    }
}
