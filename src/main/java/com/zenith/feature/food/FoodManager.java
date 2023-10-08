package com.zenith.feature.food;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zenith.Shared.OBJECT_MAPPER;
import static java.util.Objects.nonNull;

public class FoodManager {
    // key = item ID
    private Map<Integer, FoodData> foodDataMap = Collections.emptyMap();

    public FoodManager() {
        init();
    }

    public boolean isFood(final Integer id) {
        return foodDataMap.containsKey(id);
    }

    public boolean isSafeFood(final Integer id) {
        final FoodData foodData = getFoodData(id);
        if (nonNull(foodData)) {
            return !Objects.equals(foodData.getName(), "chorus_fruit")
                    && !Objects.equals(foodData.getName(), "rotten_flesh")
                    && !Objects.equals(foodData.getName(), "spider_eye")
                    && !Objects.equals(foodData.getName(), "poisonous_potato");
        }
        return false;
    }

    public FoodData getFoodData(final Integer id) {
        return foodDataMap.get(id);
    }

    private void init() {
        try {
            this.foodDataMap = OBJECT_MAPPER.readValue(getClass().getResourceAsStream("/pc/1.20/foods.json"), new TypeReference<List<FoodData>>() {
                    }).stream()
                    .collect(Collectors.toMap(FoodData::getId, v -> v, (k1, k2) -> k1));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
