package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class MoveEntityRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch());
            return true;
        } else {
            CLIENT_LOG.debug("Received ServerEntityRotationPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }
}
