package com.zenith.feature.food;

import static java.util.Objects.nonNull;

public class FoodManager {

    public FoodManager() {
    }

    public boolean isFood(final int id) {
        return FoodRegistry.REGISTRY.getIdMap().containsKey(id);
    }

    public boolean isSafeFood(final int id) {
        final FoodData foodData = getFoodData(id);
        if (nonNull(foodData)) {
            return foodData.isSafeFood();
        }
        return false;
    }

    public FoodData getFoodData(final int id) {
        return FoodRegistry.REGISTRY.get(id);
    }
}
