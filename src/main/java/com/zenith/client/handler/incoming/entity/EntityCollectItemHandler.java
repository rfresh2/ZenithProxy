package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityCollectItemPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static java.util.Objects.nonNull;

public class EntityCollectItemHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityCollectItemPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityCollectItemPacket packet, @NonNull ClientSession session) {
        if (nonNull(CACHE.getEntityCache().get(packet.getCollectedEntityId()))) {
            CACHE.getEntityCache().remove(packet.getCollectedEntityId());
            return true;
        }
        return false;
    }

    @Override
    public Class<ServerEntityCollectItemPacket> getPacketClass() {
        return ServerEntityCollectItemPacket.class;
    }
}
