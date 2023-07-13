package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class EntityPropertiesHandler implements AsyncIncomingHandler<ServerEntityPropertiesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityPropertiesPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.setProperties(packet.getAttributes());
        return true;
    }
    @Override
    public Class<ServerEntityPropertiesPacket> getPacketClass() {
        return ServerEntityPropertiesPacket.class;
    }
}
