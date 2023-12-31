package com.zenith.feature.food;

import com.fasterxml.jackson.core.type.TypeReference;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;
import java.util.Objects;

import static com.zenith.Shared.OBJECT_MAPPER;
import static java.util.Objects.nonNull;

public class FoodManager {
    // key = item ID
    private Int2ObjectMap<FoodData> foodDataMap = new Int2ObjectOpenHashMap<>();

    public FoodManager() {
        init();
    }

    public boolean isFood(final int id) {
        return foodDataMap.containsKey(id);
    }

    public boolean isSafeFood(final int id) {
        final FoodData foodData = getFoodData(id);
        if (nonNull(foodData)) {
            return !Objects.equals(foodData.getName(), "chorus_fruit")
                    && !Objects.equals(foodData.getName(), "rotten_flesh")
                    && !Objects.equals(foodData.getName(), "spider_eye")
                    && !Objects.equals(foodData.getName(), "poisonous_potato");
        }
        return false;
    }

    public FoodData getFoodData(final int id) {
        return foodDataMap.get(id);
    }

    private void init() {
        try {
            OBJECT_MAPPER.readValue(getClass().getResourceAsStream("/mcdata/foods.json"), new TypeReference<List<FoodData>>() {} )
                .forEach(foodData -> foodDataMap.put(foodData.getId().intValue(), foodData));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
