package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityPositionRotationHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityPositionRotationPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityPositionRotationPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setX(entity.getX() + packet.getMovementX())
                    .setY(entity.getY() + packet.getMovementY())
                    .setZ(entity.getZ() + packet.getMovementZ());
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntityPositionRotationPacket for invalid entity (id=%d)", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntityPositionRotationPacket> getPacketClass() {
        return ServerEntityPositionRotationPacket.class;
    }
}
