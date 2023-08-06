package com.zenith.feature.food;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class FoodManager {
    private final ObjectMapper objectMapper;
    // key = item ID
    private Map<Integer, FoodData> foodDataMap = Collections.emptyMap();

    public FoodManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        init();
    }

    public boolean isFood(final Integer id) {
        return foodDataMap.containsKey(id);
    }

    public boolean isSafeFood(final Integer id, final Integer metadata) {
        final FoodData foodData = getFoodData(id);
        if (nonNull(foodData)) {
            return !Objects.equals(foodData.getName(), "chorus_fruit")
                    && !Objects.equals(foodData.getName(), "rotten_flesh")
                    && !Objects.equals(foodData.getName(), "spider_eye")
                    && !Objects.equals(foodData.getName(), "poisonous_potato")
                    && !((Objects.equals(foodData.getName(), "fish") || (Objects.equals(foodData.getName(), "cooked_fish"))) && (metadata == 2 || metadata == 3));
        }
        return false;
    }

    public FoodData getFoodData(final Integer id) {
        return foodDataMap.get(id);
    }

    private void init() {
        try {
            this.foodDataMap = objectMapper.readValue(getClass().getResourceAsStream("/pc/1.12/foods.json"), new TypeReference<List<FoodData>>() {
                    }).stream()
                    .collect(Collectors.toMap(FoodData::getId, v -> v, (k1, k2) -> k1));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
