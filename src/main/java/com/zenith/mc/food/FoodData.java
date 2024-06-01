package com.zenith.mc.food;

import com.zenith.mc.RegistryData;

public record FoodData(
    int id,
    String name,
    int stackSize,
    float foodPoints,
    float saturation,
    boolean isSafeFood
) implements RegistryData { }
