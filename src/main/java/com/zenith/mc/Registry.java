package com.zenith.mc;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Registry<T extends RegistryData> {
    private final Int2ObjectOpenHashMap<T> idMap;

    public Registry(int size) {
        idMap = new Int2ObjectOpenHashMap<>(size);
    }

    public T register(@NotNull T value) {
        idMap.put(value.id(), value);
        return value;
    }

    public T get(int id) {
        return idMap.get(id);
    }

    public T get(@NotNull String name) {
        for (Int2ObjectMap.Entry<T> entry : idMap.int2ObjectEntrySet()) {
            if (name.equals(entry.getValue().name())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public int size() {
        return idMap.size();
    }
}
