package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.cache.data.PlayerCache;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SpectatorEntityEndCrystal extends SpectatorMob {

    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return metadataListOf(
            new BooleanEntityMetadata(9, MetadataType.BOOLEAN, false) // no beam
        );
    }

    @Override
    public double getEyeHeight() {
        return 1.5;
    }

    @Override
    public double getHeight() {
        return 2;
    }

    @Override
    public double getWidth() {
        return 2;
    }

    @Override
    EntityType getType() {
        return EntityType.END_CRYSTAL;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        var f1 = ThreadLocalRandom.current().nextFloat();
        var f2 = ThreadLocalRandom.current().nextFloat();
        return Optional.of(new ClientboundSoundPacket(
            BuiltinSound.ENTITY_GENERIC_EXPLODE,
            SoundCategory.BLOCK,
            playerCache.getX(),
            playerCache.getY(),
            playerCache.getZ(),
            4.0f,
            (1.0F + (f1 - f2) * 0.2F) * 0.7F,
            0L
        ));
    }
}
