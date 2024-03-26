package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class MoveEntityPosHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityPosPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityPosPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.setX(entity.getX() + packet.getMoveX())
                .setY(entity.getY() + packet.getMoveY())
                .setZ(entity.getZ() + packet.getMoveZ());
        return true;
    }
}
