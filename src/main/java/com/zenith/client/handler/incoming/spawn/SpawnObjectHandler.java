package com.zenith.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.zenith.cache.data.entity.EntityArmorStand;
import com.zenith.cache.data.entity.EntityObject;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class SpawnObjectHandler implements HandlerRegistry.AsyncIncomingHandler<ServerSpawnObjectPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSpawnObjectPacket packet, @NonNull ClientSession session) {
        EntityObject entity;
        if (packet.getType() == ObjectType.ARMOR_STAND) {
            entity = new EntityArmorStand();
        } else {
            entity = new EntityObject();
        }
        CACHE.getEntityCache().add(entity
                .setObjectType(packet.getType())
                .setData(packet.getData())
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUUID())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setVelX(packet.getMotionX())
                .setVelY(packet.getMotionY())
                .setVelZ(packet.getMotionZ())
        );
        return true;
    }

    @Override
    public Class<ServerSpawnObjectPacket> getPacketClass() {
        return ServerSpawnObjectPacket.class;
    }
}
