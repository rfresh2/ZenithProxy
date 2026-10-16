package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class MoveEntityPosHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityPosPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityPosPacket packet, @NonNull ClientSession session) {
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
                    entity.lerpTargetYaw(),
                    entity.lerpTargetPitch(),
                    3
                );
            }
        }
        return true;
    }
}
