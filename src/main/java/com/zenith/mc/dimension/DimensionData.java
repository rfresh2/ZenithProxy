package com.zenith.mc.dimension;

import com.zenith.mc.RegistryData;

public record DimensionData(
    int id,
    String name,
    int minY,
    int buildHeight,
    int height
) implements RegistryData { }
