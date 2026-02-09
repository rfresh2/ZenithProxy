package com.zenith.mc.item;

import com.zenith.mc.RegistryData;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jspecify.annotations.Nullable;

public record SerializedItemData(
    int id,
    String name,
    int stackSize,
    Int2ObjectArrayMap<String> components,
    @Nullable ToolTag toolTag
) implements RegistryData { }
