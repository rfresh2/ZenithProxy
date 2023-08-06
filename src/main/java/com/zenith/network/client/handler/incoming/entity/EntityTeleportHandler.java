package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class EntityTeleportHandler implements AsyncIncomingHandler<ServerEntityTeleportPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityTeleportPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setX(packet.getX())
                    .setY(packet.getY())
                    .setZ(packet.getZ())
                    .setYaw(packet.getYaw())
                    .setPitch(packet.getPitch());
            EntityPositionRotationHandler.trackPlayerVisualRangePosition(entity);
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntityTeleportPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntityTeleportPacket> getPacketClass() {
        return ServerEntityTeleportPacket.class;
    }
}
