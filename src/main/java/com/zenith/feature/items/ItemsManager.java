package com.zenith.feature.items;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Iterator;
import java.util.Locale;

import static com.zenith.Shared.OBJECT_MAPPER;

public class ItemsManager {
    private final Int2ObjectMap<ItemData> itemsData = new Int2ObjectOpenHashMap<>(1312);

    public ItemsManager() {
        init();
    }

    public void init() {
        try (JsonParser itemParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/items.json"))) {
            TreeNode node = itemParser.getCodec().readTree(itemParser);
            for (Iterator<JsonNode> it = ((ArrayNode) node).elements(); it.hasNext(); ) {
                final var e = it.next();
                int itemId = e.get("id").asInt();
                String itemName = e.get("name").asText();
                int stackSize = e.get("stackSize").asInt();
                itemsData.put(itemId, new ItemData(itemId, itemName, stackSize));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getItemId(final String itemName) {
        return itemsData.int2ObjectEntrySet().stream()
            .filter(e -> e.getValue().name().equals(itemName))
            .map(Int2ObjectMap.Entry::getIntKey)
            .findFirst()
            .orElse(-1);
    }

    public IntList getItemsContaining(final String nameChars) {
        final IntList result = new IntArrayList();
        for (Int2ObjectMap.Entry<ItemData> e : itemsData.int2ObjectEntrySet()) {
            if (e.getValue().name().contains(nameChars.toLowerCase(Locale.ROOT)))
                result.add(e.getIntKey());
        }
        return result;
    }

    public ItemData getItemData(final int id) {
        return itemsData.get(id);
    }
}
