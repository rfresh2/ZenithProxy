package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;

import static com.zenith.Shared.CACHE;

public class MoveEntityPosRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityPosRotPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityPosRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setX(entity.getX() + packet.getMoveX())
                    .setY(entity.getY() + packet.getMoveY())
                    .setZ(entity.getZ() + packet.getMoveZ());
            return true;
        } else {
//            CLIENT_LOG.warn("Received ServerEntityPositionRotationPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }
}
