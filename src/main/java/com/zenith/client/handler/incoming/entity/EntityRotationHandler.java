package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityRotationHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityRotationPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityRotationPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch());
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntityRotationPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntityRotationPacket> getPacketClass() {
        return ServerEntityRotationPacket.class;
    }
}
