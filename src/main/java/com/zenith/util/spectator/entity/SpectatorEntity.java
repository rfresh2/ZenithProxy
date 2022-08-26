package com.zenith.util.spectator.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;

import java.util.UUID;

public abstract class SpectatorEntity {

    abstract EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId);

    abstract EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId);

    public abstract Packet getSelfSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile);
    public abstract Packet getSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile);
}
