package com.zenith.feature.spectator.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;

import java.util.Optional;
import java.util.UUID;

public abstract class SpectatorEntity {
    public abstract EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId);

    public abstract EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId);

    public abstract Packet getSpawnPacket(final int entityId, final UUID uuid, final PlayerCache playerCache, final GameProfile gameProfile);
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.empty();
    }
    // A list of all minecraft mobs with eye height, total height, and total width (on 1.20)
    // https://gist.github.com/bradcarnage/c894976345a0e57280c8619fe3ac0282
    public abstract double getEyeHeight();
    public abstract double getHeight();
    public abstract double getWidth();
}
