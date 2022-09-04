package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static java.util.Objects.isNull;

public class EntityHeadLookHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityHeadLookPacket, ClientSession> {
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
