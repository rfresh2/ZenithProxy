package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class EntityMetadataHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityMetadataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityMetadataPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        MAINLOOP:
        for (EntityMetadata metadata : packet.getMetadata())    {
            for (int i = entity.getMetadata().size() - 1; i >= 0; i--)  {
                EntityMetadata old = entity.getMetadata().get(i);
                if (old.getId() == metadata.getId())    {
                    entity.getMetadata().set(i, metadata);
                    continue MAINLOOP;
                }
            }
            entity.getMetadata().add(metadata);
        }
        return true;
    }

    @Override
    public Class<ServerEntityMetadataPacket> getPacketClass() {
        return ServerEntityMetadataPacket.class;
    }
}
