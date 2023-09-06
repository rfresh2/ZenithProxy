package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.object.GenericObjectData;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;
import com.zenith.feature.spectator.entity.SpectatorEntity;

import java.util.UUID;

public abstract class SpectatorMob extends SpectatorEntity {
    abstract EntityType getType();

    @Override
    public Packet getSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile) {
        return new ClientboundAddEntityPacket(
            entityId,
            uuid,
            getType(),
            new GenericObjectData(0),
            playerCache.getX(),
            playerCache.getY(),
            playerCache.getZ(),
            playerCache.getYaw(),
            playerCache.getYaw(), // todo: head yaw
            playerCache.getPitch(),
            0f,
            0f,
            0f);
    }
}
