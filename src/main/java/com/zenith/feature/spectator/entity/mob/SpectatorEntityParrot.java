package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.SoundCategory;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SpectatorEntityParrot extends SpectatorMob {
    @Override
    public EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorRealProfile, GameProfile spectatorFakeProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, true);
    }

    @Override
    public EntityMetadata[] getEntityMetadata(final GameProfile spectatorRealProfile, GameProfile spectatorFakeProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, false);
    }

    private EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId, final boolean self) {
        return new EntityMetadata[]{
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new IntEntityMetadata(19, MetadataType.INT, (spectatorEntityId % 5)) // variant
        };
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
        final float randFloat = ThreadLocalRandom.current().nextFloat();
        return Optional.of(new ClientboundSoundPacket(
            BuiltinSound.ENTITY_PARROT_AMBIENT,
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
