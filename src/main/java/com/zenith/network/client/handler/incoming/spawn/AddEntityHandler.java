package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class AddEntityHandler implements AsyncIncomingHandler<ClientboundAddEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundAddEntityPacket packet, @NonNull ClientSession session) {
        CACHE.getEntityCache().add(new EntityStandard()
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
                                       .setVelZ(packet.getMotionZ())
        );
        return true;
    }
}
