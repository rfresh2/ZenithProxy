package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.cache.data.PlayerCache;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.asList;

public class SpectatorEntityEnderDragon extends SpectatorMob {
    @Override
    public List<EntityMetadata<?, ?>> getSelfEntityMetadata(final GameProfile spectatorRealProfile, GameProfile spectatorFakeProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, true);
    }

    @Override
    public List<EntityMetadata<?, ?>> getEntityMetadata(final GameProfile spectatorRealProfile, GameProfile spectatorFakeProfile, int spectatorEntityId) {
        return getEntityMetadata(spectatorFakeProfile, spectatorEntityId, false);
    }

    private List<EntityMetadata<?, ?>> getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId, final boolean self) {
        return asList(
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new IntEntityMetadata(16, MetadataType.INT, 10) // dragon phase
        );
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
