package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class EntityHeadLookHandler implements AsyncIncomingHandler<ServerEntityHeadLookPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityHeadLookPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.setHeadYaw(packet.getHeadYaw());
        return true;
    }

    @Override
    public Class<ServerEntityHeadLookPacket> getPacketClass() {
        return ServerEntityHeadLookPacket.class;
    }
}
