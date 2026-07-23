package com.zenith.feature.spectator.entity.mob;

import com.zenith.cache.data.PlayerCache;
import com.zenith.mc.entity.EntityRegistry;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.Optional;

public class SpectatorEntityWither extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return noMetadataList();
    }

    @Override
    EntityType getType() {
        return EntityType.WITHER;
    }

    @Override
    public double getEyeHeight() {
        return EntityRegistry.WITHER.eyeHeight();
    }

    @Override
    public double getHeight() {
        return EntityRegistry.WITHER.height();
    }

    @Override
    public double getWidth() {
        return EntityRegistry.WITHER.width();
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_WITHER_SHOOT, BuiltinSound.ENTITY_WITHER_AMBIENT));
    }
}
