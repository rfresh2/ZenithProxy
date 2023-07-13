package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class EntityRotationHandler implements AsyncIncomingHandler<ServerEntityRotationPacket, ClientSession> {
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
