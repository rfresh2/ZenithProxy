package com.zenith.mc.dimension;

import com.zenith.mc.RegistryData;

public record DimensionData(
    int id,
    String name,
    int minY,
    int buildHeight,
    int height
) implements RegistryData {
    public int sectionCount() {
        return maxSection() - minSection();
    }

    public int minSection() {
        return minY >> 4;
    }

    public int maxSection() {
        return ((buildHeight - 1) >> 4) + 1;
    }
}
