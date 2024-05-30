package com.zenith.mc.dimension;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DimensionDataManager {
    private final Map<String, DimensionData> dimensionNameToData = new ConcurrentHashMap<>(4);

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
        return dimensionNameToData.get(name);
    }

    public Collection<String> dimensionNames() {
        return dimensionNameToData.keySet();
    }
}
