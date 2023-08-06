package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.google.common.collect.Lists;
import com.zenith.cache.data.entity.EntityMob;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SpawnMobHandler implements AsyncIncomingHandler<ServerSpawnMobPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSpawnMobPacket packet, @NonNull ClientSession session) {
        CACHE.getEntityCache().add(new EntityMob()
                .setMobType(packet.getType())
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUUID())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setHeadYaw(packet.getHeadYaw())
                .setVelX(packet.getMotionX())
                .setVelY(packet.getMotionY())
                .setVelZ(packet.getMotionZ())
                .setMetadata(Lists.newArrayList(packet.getMetadata()))
        );
        return true;
    }

    @Override
    public Class<ServerSpawnMobPacket> getPacketClass() {
        return ServerSpawnMobPacket.class;
    }
}
