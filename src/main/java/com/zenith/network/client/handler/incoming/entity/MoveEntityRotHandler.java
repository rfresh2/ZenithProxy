package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class MoveEntityRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            var isControlledByLocalInstance = entity.isControlledByLocalInstance();
            if (!isControlledByLocalInstance) {
                entity.lerpTo(
                    entity.lerpTargetX(),
                    entity.lerpTargetY(),
                    entity.lerpTargetZ(),
                    packet.getYaw(),
                    packet.getPitch(),
                    3
                );
            }
        }
        return true;
    }
}
