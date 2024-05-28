package com.zenith.mc.dimension;

import com.zenith.util.Registry;

public final class DimensionRegistry {
    public static final Registry<DimensionData> REGISTRY = new Registry<>(4);

    public static final DimensionData OVERWORLD = register(new DimensionData(0, "overworld", -64, 320, 384));

    public static final DimensionData OVERWORLD_CAVES = register(new DimensionData(1, "overworld_caves", -64, 320, 384));

    public static final DimensionData THE_END = register(new DimensionData(2, "the_end", 0, 256, 256));

    public static final DimensionData THE_NETHER = register(new DimensionData(3, "the_nether", 0, 256, 256));

    private static DimensionData register(DimensionData value) {
        REGISTRY.put(value.id(), value);
        return value;
    }
}
