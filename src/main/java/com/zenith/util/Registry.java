package com.zenith.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class Registry<T> {
    private final Int2ObjectOpenHashMap<T> idMap;

    public Registry(int size) {
        idMap = new Int2ObjectOpenHashMap<>(size);
    }

    public Registry() {
        idMap = new Int2ObjectOpenHashMap<>();
    }

    public void put(int id, T value) {
        idMap.put(id, value);
    }

    public @Nullable T get(int id) {
        return idMap.get(id);
    }
}
