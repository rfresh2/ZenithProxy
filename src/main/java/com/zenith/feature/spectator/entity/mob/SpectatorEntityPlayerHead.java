package com.zenith.feature.spectator.entity.mob;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.Shared;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.ObjectDataComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        final Map<DataComponentType<?>, DataComponent<?, ?>> dataComponentsMap = new HashMap<>();
        dataComponentsMap.put(DataComponentType.PROFILE, new ObjectDataComponent<GameProfile>(DataComponentType.PROFILE, spectatorProfile));
        final DataComponents dataComponents = new DataComponents(dataComponentsMap);
        return new EntityMetadata[]{
            new ObjectEntityMetadata<>(2, MetadataType.OPTIONAL_CHAT, Optional.of(Component.text(spectatorProfile.getName()))),
            new BooleanEntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
            new ObjectEntityMetadata<>(23, MetadataType.ITEM, new ItemStack(playerHeadItemId, 1, dataComponents))
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
