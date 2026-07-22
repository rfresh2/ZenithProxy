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

public class SpectatorEntityBat extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return noMetadataList();
    }

    @Override
    EntityType getType() {
        return EntityType.BAT;
    }

    @Override
    public double getEyeHeight() {
        return EntityRegistry.BAT.eyeHeight();
    }

    @Override
    public double getHeight() {
        return EntityRegistry.BAT.height();
    }

    @Override
    public double getWidth() {
        return EntityRegistry.BAT.width();
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_BAT_AMBIENT));
    }
}
