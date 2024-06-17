package com.zenith.feature.spectator.entity.mob;

import com.zenith.cache.data.PlayerCache;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.Optional;

public class SpectatorEntityCat extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return metadataListOf(
            new ObjectEntityMetadata<>(19, MetadataType.CAT_VARIANT, (spectatorEntityId % 10))
        );
    }

    @Override
    EntityType getType() {
        return EntityType.CAT;
    }

    @Override
    public double getEyeHeight() {
        return 0.35;
    }

    @Override
    public double getHeight() {
        return 0.7;
    }

    @Override
    public double getWidth() {
        return 0.6;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_CAT_PURREOW, BuiltinSound.ENTITY_CAT_AMBIENT));
    }

}
