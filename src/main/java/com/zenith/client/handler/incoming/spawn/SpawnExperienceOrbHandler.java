package com.zenith.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.zenith.cache.data.entity.EntityExperienceOrb;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class SpawnExperienceOrbHandler implements HandlerRegistry.AsyncIncomingHandler<ServerSpawnExpOrbPacket, ClientSession> {
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
