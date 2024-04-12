package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class RotateHeadHandler implements ClientEventLoopPacketHandler<ClientboundRotateHeadPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundRotateHeadPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.setHeadYaw(packet.getHeadYaw());
        return true;
    }
}
