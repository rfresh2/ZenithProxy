package com.zenith.network.client.handler.incoming.spawn;

import com.zenith.cache.data.entity.EntityExperienceOrb;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class AddExperienceOrbHandler implements ClientEventLoopPacketHandler<ClientboundAddExperienceOrbPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundAddExperienceOrbPacket packet, @NonNull ClientSession session) {
        CACHE.getEntityCache().add(
            new EntityExperienceOrb()
                .setExp(packet.getExp())
                .setEntityId(packet.getEntityId())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setBaseX(packet.getX())
                .setBaseY(packet.getY())
                .setBaseZ(packet.getZ())
                .setEntityType(EntityType.EXPERIENCE_ORB)
        );
        return true;
    }
}
