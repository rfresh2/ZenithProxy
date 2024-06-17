package com.zenith.feature.spectator.entity.mob;

import com.viaversion.nbt.io.MNBTIO;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.ArrayList;

import static com.zenith.Shared.SERVER_LOG;

public class SpectatorEntityPlayerHead extends SpectatorMob {
    // example command to summon a player head:
    // /summon minecraft:item_display ~ ~ ~ {item:{id:"minecraft:player_head",Count:1b,tag:{SkullOwner:"rfresh2"}}}
    // the uuid and textures get populated by the server usually. but in this case, we're the server

    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
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
            var texturesTag = new ListTag<>(CompoundTag.class);
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
        return metadataListOf(
            new ObjectEntityMetadata<>(23, MetadataType.ITEM, new ItemStack(ItemRegistry.PLAYER_HEAD.id(), 1, mnbt))
        );
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
