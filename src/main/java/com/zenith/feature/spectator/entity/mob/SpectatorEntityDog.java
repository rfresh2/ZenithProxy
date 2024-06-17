package com.zenith.feature.spectator.entity.mob;

import com.zenith.cache.data.PlayerCache;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.Optional;

public class SpectatorEntityDog extends SpectatorMob {
    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return noMetadataList();
        // todo: need to create wolf variant registry in dataGenerator for this to work in 1.21+
        //  cba just for this use case though lol
//        return metadataListOf(
//            new ObjectEntityMetadata<Holder<WolfVariant>>(22, MetadataType.WOLF_VARIANT, ThreadLocalRandom.current().nextInt(0, 9))
//        );
    }

    @Override
    EntityType getType() {
        return EntityType.WOLF;
    }

    @Override
    public double getEyeHeight() {
        return 0.68;
    }

    @Override
    public double getHeight() {
        return 0.85;
    }

    @Override
    public double getWidth() {
        return 0.6;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        return Optional.of(buildSoundPacket(playerCache, BuiltinSound.ENTITY_WOLF_WHINE, BuiltinSound.ENTITY_WOLF_AMBIENT));
    }
}
