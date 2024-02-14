package com.zenith.feature.items;

import com.fasterxml.jackson.core.type.TypeReference;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.Locale;

import static com.zenith.Shared.OBJECT_MAPPER;

public class ItemsManager {
    private final Int2ObjectMap<ItemsData> itemsData = new Int2ObjectOpenHashMap<>();

    public ItemsManager() {
        init();
    }

    public void init() {
        try {
            OBJECT_MAPPER.readValue(getClass().getResourceAsStream("/mcdata/items.json"), new TypeReference<List<ItemsData>>() {} )
                .forEach(data -> itemsData.put(data.getId(), data));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getItemId(final String itemName) {
        return itemsData.int2ObjectEntrySet().stream()
            .filter(e -> e.getValue().getName().equals(itemName))
            .map(Int2ObjectMap.Entry::getIntKey)
            .findFirst()
            .orElse(-1);
    }

    public IntList getItemsContaining(final String nameChars) {
        final IntList result = new IntArrayList();
        for (Int2ObjectMap.Entry<ItemsData> e : itemsData.int2ObjectEntrySet()) {
            if (e.getValue().getName().contains(nameChars.toLowerCase(Locale.ROOT)))
                result.add(e.getIntKey());
        }
        return result;
    }

    public ItemsData getItemData(final int id) {
        return itemsData.get(id);
    }
}
