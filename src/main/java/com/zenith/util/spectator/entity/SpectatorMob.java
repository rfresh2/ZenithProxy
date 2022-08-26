package com.zenith.util.spectator.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;

import java.util.UUID;

public abstract class SpectatorMob extends SpectatorEntity {

    abstract MobType getMobType();

    @Override
    public Packet getSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile) {
        return new ServerSpawnMobPacket(
                entityId,
                uuid,
                getMobType(),
                playerCache.getX(),
                playerCache.getY(),
                playerCache.getZ(),
                playerCache.getYaw(),
                playerCache.getPitch(),
                playerCache.getYaw(),
                0f,
                0f,
                0f,
                getEntityMetadata(gameProfile, entityId));
    }

    @Override
    public Packet getSelfSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile) {
        return new ServerSpawnMobPacket(
                entityId,
                uuid,
                getMobType(),
                playerCache.getX(),
                playerCache.getY(),
                playerCache.getZ(),
                playerCache.getYaw(),
                playerCache.getPitch(),
                playerCache.getYaw(),
                0f,
                0f,
                0f,
                getSelfEntityMetadata(gameProfile, entityId));
    }
}
