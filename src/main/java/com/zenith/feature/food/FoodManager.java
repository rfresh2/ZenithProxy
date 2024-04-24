package com.zenith.feature.food;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Iterator;
import java.util.Objects;

import static com.zenith.Shared.OBJECT_MAPPER;
import static java.util.Objects.nonNull;

public class FoodManager {
    // key = item ID
    private Int2ObjectMap<FoodData> foodDataMap = new Int2ObjectOpenHashMap<>(40);

    public FoodManager() {
        init();
    }

    public boolean isFood(final int id) {
        return foodDataMap.containsKey(id);
    }

    public boolean isSafeFood(final int id) {
        final FoodData foodData = getFoodData(id);
        if (nonNull(foodData)) {
            return !Objects.equals(foodData.name(), "chorus_fruit")
                    && !Objects.equals(foodData.name(), "rotten_flesh")
                    && !Objects.equals(foodData.name(), "spider_eye")
                    && !Objects.equals(foodData.name(), "poisonous_potato");
        }
        return false;
    }

    public FoodData getFoodData(final int id) {
        return foodDataMap.get(id);
    }

    private void init() {
        try (JsonParser foodParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/foods.json"))) {
            TreeNode node = foodParser.getCodec().readTree(foodParser);
            for (Iterator<JsonNode> it = ((ArrayNode) node).elements(); it.hasNext(); ) {
                final var e = it.next();
                int itemId = e.get("id").asInt();
                String itemName = e.get("name").asText();
                int stackSize = e.get("stackSize").asInt();
                double foodPoints = e.get("foodPoints").asDouble();
                double saturation = e.get("saturation").asDouble();
                foodDataMap.put(itemId, new FoodData(itemId, itemName, stackSize, foodPoints, saturation));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
