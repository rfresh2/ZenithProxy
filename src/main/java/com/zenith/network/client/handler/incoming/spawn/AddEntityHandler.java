package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class AddEntityHandler implements AsyncIncomingHandler<ClientboundAddEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundAddEntityPacket packet, @NonNull ClientSession session) {
        final EntityStandard entity = (EntityStandard) new EntityStandard()
            .setEntityType(packet.getType())
            .setObjectData(packet.getData())
            .setEntityId(packet.getEntityId())
            .setUuid(packet.getUuid())
            .setX(packet.getX())
            .setY(packet.getY())
            .setZ(packet.getZ())
            .setYaw(packet.getYaw())
            .setPitch(packet.getPitch())
            .setHeadYaw(packet.getHeadYaw())
            .setVelX(packet.getMotionX())
            .setVelY(packet.getMotionY())
            .setVelZ(packet.getMotionZ());
        CACHE.getEntityCache().remove(packet.getEntityId());
        CACHE.getEntityCache().add(entity);
        if (entity.getEntityType() == EntityType.FISHING_BOBBER) {
            EVENT_BUS.postAsync(new EntityFishHookSpawnEvent(entity));
        }
        return true;
    }
}
