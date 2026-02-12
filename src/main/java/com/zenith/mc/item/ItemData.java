package com.zenith.mc.item;

import com.zenith.mc.RegistryData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.EnumSet;

public record ItemData(
    int id,
    String name,
    @JsonDeserialize(using = ItemDataComponentsDeserializer.class)
    DataComponents components,
    EnumSet<ItemTags> itemTags,
    @Nullable ToolTag toolTag
) implements RegistryData {

    public int stackSize() {
        return components().getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
    }
}
