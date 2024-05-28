package com.zenith.mc.food;

public record FoodData(
    int id,
    String name,
    int stackSize,
    float foodPoints,
    float saturation,
    boolean isSafeFood
) { }
