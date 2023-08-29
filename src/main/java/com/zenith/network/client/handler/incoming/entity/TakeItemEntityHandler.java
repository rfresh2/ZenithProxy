package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTakeItemEntityPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.nonNull;

public class TakeItemEntityHandler implements AsyncIncomingHandler<ClientboundTakeItemEntityPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundTakeItemEntityPacket packet, @NonNull ClientSession session) {
        if (nonNull(CACHE.getEntityCache().get(packet.getCollectedEntityId()))) {
            CACHE.getEntityCache().remove(packet.getCollectedEntityId());
            return true;
        }
        return false;
    }

    @Override
    public Class<ClientboundTakeItemEntityPacket> getPacketClass() {
        return ClientboundTakeItemEntityPacket.class;
    }
}
