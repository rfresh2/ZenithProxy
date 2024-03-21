package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.io.MNBTIO;
import com.zenith.Shared;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;

import java.util.Optional;

import static com.zenith.Shared.SERVER_LOG;

public class SpectatorEntityPlayerHead extends SpectatorMob {
    // example command to summon a player head:
    // /summon minecraft:item_display ~ ~ ~ {item:{id:"minecraft:player_head",Count:1b,tag:{SkullOwner:"rfresh2"}}}
    // the uuid and textures get populated by the server usually. but in this case, we're the server

    private int playerHeadItemId = Shared.ITEMS.getItemId("player_head");

    @Override
    public EntityMetadata[] getSelfEntityMetadata(final GameProfile spectatorRealProfile, final GameProfile spectatorFakeProfile, final int spectatorEntityId) {
        return getEntityMetadata(spectatorRealProfile, spectatorEntityId, true);
    }

    @Override
    public EntityMetadata[] getEntityMetadata(final GameProfile spectatorRealProfile, final GameProfile spectatorFakeProfile, final int spectatorEntityId) {
        return getEntityMetadata(spectatorRealProfile, spectatorEntityId, false);
    }

    @SneakyThrows
    private EntityMetadata[] getEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId, final boolean self) {
        var tag = new CompoundTag();
        var ownerTag = new CompoundTag();
        tag.put("SkullOwner", ownerTag);
        ownerTag.putString("Name", spectatorProfile.getName());
        var uuidTag = new IntArrayTag();
        var spectUuid = spectatorProfile.getId();
        var most = spectUuid.getMostSignificantBits();
        var least = spectUuid.getLeastSignificantBits();
        uuidTag.setValue(new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
        ownerTag.put("Id", uuidTag);
        try {
            var propertiesTag = new CompoundTag();
            var texturesTag = new ListTag(CompoundTag.class);
            var textureProperty = spectatorProfile.getProperty("textures");
            propertiesTag.put("textures", texturesTag);
            var textureValueTag = new CompoundTag();
            textureValueTag.putString("Value", textureProperty.getValue());
            textureValueTag.putString("Signature", textureProperty.getSignature());
            texturesTag.add(textureValueTag);
            ownerTag.put("Properties", propertiesTag);
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed to get textures for player head spectator entity", e);
        }
        var mnbt = MNBTIO.write(tag, false);
        return new EntityMetadata[]{
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new ObjectEntityMetadata<>(23, MetadataType.ITEM, new ItemStack(playerHeadItemId, 1, mnbt))
        };
    }

    @Override
    EntityType getType() {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    public double getEyeHeight() {
        return -0.5; // getting the y pos of the head to be a bit higher than default
    }

    @Override
    public double getHeight() {
        return 0.3;
    }

    @Override
    public double getWidth() {
        return 0.3;
    }
}
