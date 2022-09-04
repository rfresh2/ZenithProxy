package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityTeleportHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityTeleportPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityTeleportPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setX(packet.getX())
                    .setY(packet.getY())
                    .setZ(packet.getZ())
                    .setYaw(packet.getYaw())
                    .setPitch(packet.getPitch());
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
