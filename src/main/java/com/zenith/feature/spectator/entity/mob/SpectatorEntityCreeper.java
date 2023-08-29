package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.*;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.data.PlayerCache;

import java.util.Optional;

public class SpectatorEntityCreeper extends SpectatorMob {
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
                new ByteEntityMetadata(0, MetadataType.BYTE, (byte) 0),
                new IntEntityMetadata(1, MetadataType.INT, 0),
                new ObjectEntityMetadata<String>(2, MetadataType.STRING, spectatorProfile.getName()),
                new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
                new BooleanEntityMetadata(4, MetadataType.BOOLEAN, false),
                new BooleanEntityMetadata(5, MetadataType.BOOLEAN, false),
                new ByteEntityMetadata(6, MetadataType.BYTE, (byte) 0),
                new FloatEntityMetadata(7, MetadataType.FLOAT, 10.0f),
                new IntEntityMetadata(8, MetadataType.INT, 0),
                new BooleanEntityMetadata(9, MetadataType.BOOLEAN, false),
                new IntEntityMetadata(10, MetadataType.INT, 0),
                new ByteEntityMetadata(11, MetadataType.BYTE, (byte) 0),
                new IntEntityMetadata(12, MetadataType.INT, -1), // -1 = idle, 1 = fuse
                new BooleanEntityMetadata(13, MetadataType.BOOLEAN, false), // is charged
                new BooleanEntityMetadata(14, MetadataType.BOOLEAN, false) // is ignited
        };
    }

    @Override
    EntityType getType() {
        return EntityType.CREEPER;
    }

    @Override
    public Optional<Packet> getSoundPacket(final PlayerCache playerCache) {
//        final float randFloat = ThreadLocalRandom.current().nextFloat();
//        return Optional.of(new ServerPlayBuiltinSoundPacket(
//                BuiltinSound.ENTITY_CREEPER_PRIMED,
//                SoundCategory.AMBIENT,
//                playerCache.getX(),
//                playerCache.getY(),
//                playerCache.getZ(),
//                1.0f - (randFloat / 2f),
//                1.0f + (randFloat / 10f) // slight pitch variations
//        ));
        return Optional.empty();
    }
}
