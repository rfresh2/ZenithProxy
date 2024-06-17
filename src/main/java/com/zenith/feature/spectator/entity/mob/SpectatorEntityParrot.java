package com.zenith.feature.spectator.entity.mob;

import com.zenith.cache.data.PlayerCache;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.Optional;

public class SpectatorEntityParrot extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return metadataListOf(
            new IntEntityMetadata(19, MetadataType.INT, (spectatorEntityId % 5)) // variant
        );
    }

    @Override
    EntityType getType() {
        return EntityType.PARROT;
    }

    @Override
    public double getEyeHeight() {
        return 0.54;
    }

    @Override
    public double getHeight() {
        return 0.9;
    }

    @Override
    public double getWidth() {
        return 0.5;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_PARROT_AMBIENT));
    }
}
