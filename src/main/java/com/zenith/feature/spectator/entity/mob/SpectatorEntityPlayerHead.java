package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.feature.items.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.ObjectDataComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SpectatorEntityPlayerHead extends SpectatorMob {
    // example command to summon a player head:
    // /summon minecraft:item_display ~ ~ ~ {item:{id:"minecraft:player_head",Count:1b,tag:{SkullOwner:"rfresh2"}}}
    // the uuid and textures get populated by the server usually. but in this case, we're the server

    @Override
    public ArrayList<EntityMetadata<?, ?>> getBaseEntityMetadata(final GameProfile spectatorProfile, final int spectatorEntityId) {
        final Map<DataComponentType<?>, DataComponent<?, ?>> dataComponentsMap = new HashMap<>();
        dataComponentsMap.put(DataComponentType.PROFILE, new ObjectDataComponent<GameProfile>(DataComponentType.PROFILE, spectatorProfile));
        final DataComponents dataComponents = new DataComponents(dataComponentsMap);
        return metadataListOf(
            new ObjectEntityMetadata<>(23, MetadataType.ITEM, new ItemStack(ItemRegistry.PLAYER_HEAD.id(), 1, dataComponents))
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
