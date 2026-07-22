package com.zenith.feature.spectator.entity.mob;

import com.zenith.cache.data.PlayerCache;
import com.zenith.mc.entity.EntityRegistry;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.Optional;

public class SpectatorEntityParrot extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return metadataListOf(
            new IntEntityMetadata(19, MetadataTypes.INT, (spectatorEntityId % 5)) // variant
        );
    }

    @Override
    EntityType getType() {
        return EntityType.PARROT;
    }

    @Override
    public double getEyeHeight() {
        return EntityRegistry.PARROT.eyeHeight();
    }

    @Override
    public double getHeight() {
        return EntityRegistry.PARROT.height();
    }

    @Override
    public double getWidth() {
        return EntityRegistry.PARROT.width();
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_PARROT_AMBIENT));
    }
}
