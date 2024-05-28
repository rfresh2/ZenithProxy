package com.zenith.feature.world.dimension;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Collection;

public class DimensionDataManager {
    private final Object2ObjectOpenHashMap<String, DimensionData> dimensionNameToData = new Object2ObjectOpenHashMap<>(4);

    public DimensionDataManager() {
        init();
    }

    private void init() {
        for (var entry : DimensionRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            dimensionNameToData.put(entry.getValue().name(), entry.getValue());
        }
    }

    public DimensionData getDimensionData(final int id) {
        return DimensionRegistry.REGISTRY.get(id);
    }

    public DimensionData getDimensionData(final String name) {
        var data = dimensionNameToData.get(name);
        if (data == dimensionNameToData.defaultReturnValue()) return null;
        return data;
    }

    public Collection<String> dimensionNames() {
        return dimensionNameToData.keySet();
    }
}
