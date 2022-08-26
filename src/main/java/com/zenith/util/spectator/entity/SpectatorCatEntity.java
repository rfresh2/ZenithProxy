package com.zenith.util.spectator.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;

public class SpectatorCatEntity extends SpectatorMob {

    @Override
    EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return new EntityMetadata[]{
                new EntityMetadata(0, MetadataType.BYTE, (byte)0),
                new EntityMetadata(1, MetadataType.INT, 0),
                new EntityMetadata(2, MetadataType.STRING, spectatorProfile.getName()),
                new EntityMetadata(3, MetadataType.BOOLEAN, false), // hide nametag on self
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte)0),
                new EntityMetadata(7, MetadataType.FLOAT, 10.0f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.BYTE, (byte)0),
                new EntityMetadata(12, MetadataType.BOOLEAN, false),
                new EntityMetadata(13, MetadataType.BYTE, (byte)4),
//                new EntityMetadata(14, MetadataType.OPTIONAL_UUID, this.getProfileCache().getProfile().getId()), // mob owner
                new EntityMetadata(15, MetadataType.INT, (spectatorEntityId % 3) + 1) // cat texture variant
        };
    }

    @Override
    EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        return new EntityMetadata[]{
                new EntityMetadata(0, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(1, MetadataType.INT, 0),
                new EntityMetadata(2, MetadataType.STRING, spectatorProfile.getName()),
                new EntityMetadata(3, MetadataType.BOOLEAN, true),
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(7, MetadataType.FLOAT, 10.0f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(12, MetadataType.BOOLEAN, false),
                new EntityMetadata(13, MetadataType.BYTE, (byte) 4),
//                new EntityMetadata(14, MetadataType.OPTIONAL_UUID, this.getProfileCache().getProfile().getId()), // mob owner
                new EntityMetadata(15, MetadataType.INT, (spectatorEntityId % 3) + 1) // cat texture variant
        };
    }

    @Override
    MobType getMobType() {
        return MobType.OCELOT;
    }


}
