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

public class SpectatorEntityEnderDragon extends SpectatorMob {
    @Override
    public EntityMetadata[] getSelfEntityMetadata(GameProfile spectatorProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorProfile, spectatorEntityId, true);
    }

    @Override
    public EntityMetadata[] getEntityMetadata(GameProfile spectatorProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorProfile, spectatorEntityId, false);
    }

    private EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId, final boolean self) {
        return new EntityMetadata[]{
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new IntEntityMetadata(16, MetadataType.INT, 10) // dragon phase
        };
    }

    @Override
    EntityType getType() {
        return EntityType.ENDER_DRAGON;
    }

    @Override
    public double getEyeHeight() {
        return 1.0;
    }
    @Override
    public double getHeight() {
        return 8.0;
    }
    @Override
    public double getWidth() {
        return 16;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        final float randFloat = ThreadLocalRandom.current().nextFloat();
        final int randInt = ThreadLocalRandom.current().nextInt(4);
        return Optional.of(new ClientboundSoundPacket(
            randInt == 0 ? BuiltinSound.ENTITY_ENDER_DRAGON_GROWL : BuiltinSound.ENTITY_ENDER_DRAGON_AMBIENT,
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
