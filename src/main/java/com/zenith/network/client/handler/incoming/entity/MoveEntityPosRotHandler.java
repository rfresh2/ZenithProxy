package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class MoveEntityPosRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityPosRotPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityPosRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            var isControlledByLocalInstance = entity.isControlledByLocalInstance();
            entity.setBaseX(entity.getBaseX() + packet.getMoveX());
            entity.setBaseY(entity.getBaseY() + packet.getMoveY());
            entity.setBaseZ(entity.getBaseZ() + packet.getMoveZ());
            if (!isControlledByLocalInstance) {
                entity.lerpTo(
                    entity.getBaseX(),
                    entity.getBaseY(),
                    entity.getBaseZ(),
                    packet.getYaw(),
                    packet.getPitch(),
                    3
                );
            }
        }
        return true;
    }
}
