package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.cache.data.PlayerCache;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SpectatorEntityCat extends SpectatorMob {
    @Override
    public EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorRealProfile, final GameProfile spectatorFakeProfile, final int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, true);
    }

    @Override
    public EntityMetadata[] getEntityMetadata(final GameProfile spectatorRealProfile, final GameProfile spectatorFakeProfile, final int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, false);
    }

    private EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId, final boolean self) {
        return new EntityMetadata[]{
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new ObjectEntityMetadata<>(19, MetadataType.CAT_VARIANT, (spectatorEntityId % 10)), // cat texture variant
        };
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
        final float randFloat = ThreadLocalRandom.current().nextFloat();
        final int randInt = ThreadLocalRandom.current().nextInt(4);
        return Optional.of(new ClientboundSoundPacket(
            randInt == 0 ? BuiltinSound.ENTITY_CAT_PURREOW : BuiltinSound.ENTITY_CAT_AMBIENT,
            SoundCategory.AMBIENT,
            playerCache.getX(),
            playerCache.getY(),
            playerCache.getZ(),
            1.0f - (randFloat / 2f),
            1.0f + (randFloat / 10f), // slight pitch variations
            0L
        ));
    }

}
