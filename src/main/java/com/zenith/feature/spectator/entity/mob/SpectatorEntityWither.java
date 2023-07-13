package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.SoundCategory;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayBuiltinSoundPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;

import java.util.Optional;

public class SpectatorEntityWither extends SpectatorMob {
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
                new EntityMetadata(0, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(1, MetadataType.INT, 0),
                new EntityMetadata(2, MetadataType.STRING, spectatorProfile.getName()),
                new EntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(7, MetadataType.FLOAT, 10.0f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(12, MetadataType.INT, 0),
                new EntityMetadata(13, MetadataType.INT, 0),
                new EntityMetadata(14, MetadataType.INT, 0),
                new EntityMetadata(15, MetadataType.INT, 0),
        };
    }

    @Override
    MobType getMobType() {
        return MobType.WITHER;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
        final float randFloat = rand.nextFloat();
        final int randInt = rand.nextInt(4);
        return Optional.of(new ServerPlayBuiltinSoundPacket(
                randInt == 0 ? BuiltinSound.ENTITY_WITHER_SHOOT : BuiltinSound.ENTITY_WITHER_AMBIENT,
                SoundCategory.AMBIENT,
                playerCache.getX(),
                playerCache.getY(),
                playerCache.getZ(),
                1.0f - (randFloat / 2f),
                1.0f + (randFloat / 10f) // slight pitch variations
        ));
    }
}
