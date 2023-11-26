package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class SetEntityDataHandler implements AsyncPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEntityDataPacket packet, @NonNull ClientSession session) {
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
}
