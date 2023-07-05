package com.zenith.spectator.entity.object;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectData;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;
import com.zenith.spectator.entity.SpectatorEntity;

import java.util.UUID;

public abstract class SpectatorEntityObject extends SpectatorEntity {

    public abstract ObjectType getObjectType();

    public abstract ObjectData getObjectData();

    @Override
    public Packet getSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile) {
        return new ServerSpawnObjectPacket(
                entityId,
                uuid,
                getObjectType(),
                getObjectData(),
                playerCache.getX(),
                playerCache.getY(),
                playerCache.getZ(),
                playerCache.getYaw(),
                playerCache.getPitch());
    }

}
