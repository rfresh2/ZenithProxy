package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class SetEntityDataHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEntityDataPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        for (int i = 0; i < packet.getMetadata().size(); i++) {
            var metadata = packet.getMetadata().get(i);
            entity.getMetadata().put(metadata.getId(), metadata);
        }
        return true;
    }
}
