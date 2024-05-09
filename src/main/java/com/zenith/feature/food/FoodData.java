package com.zenith.feature.food;

public record FoodData(
    int id,
    String name,
    int stackSize,
    double foodPoints,
    double saturation,
    boolean isSafeFood
) { }
