package com.zenith.network.client.handler.incoming.entity;

import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.BOT;
import static com.zenith.Globals.CACHE;

public class SetEntityDataHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEntityDataPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            for (int i = 0; i < packet.getMetadata().size(); i++) {
                var metadata = packet.getMetadata().get(i);
                entity.getMetadata().put(metadata.getId(), metadata);
            }
            if (!Proxy.getInstance().hasActivePlayer() && entity == CACHE.getPlayerCache().getThePlayer()) {
                BOT.onMetadataUpdated(packet.getMetadata());
            }
        }
        return true;
    }
}
