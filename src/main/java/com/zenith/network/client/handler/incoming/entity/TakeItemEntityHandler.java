package com.zenith.network.client.handler.incoming.entity;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTakeItemEntityPacket;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.nonNull;

public class TakeItemEntityHandler implements ClientEventLoopPacketHandler<ClientboundTakeItemEntityPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundTakeItemEntityPacket packet, @NonNull ClientSession session) {
        if (nonNull(CACHE.getEntityCache().get(packet.getCollectedEntityId()))) {
            CACHE.getEntityCache().remove(packet.getCollectedEntityId());
            return true;
        }
        return false;
    }

}
