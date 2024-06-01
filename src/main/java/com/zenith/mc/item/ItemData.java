package com.zenith.mc.item;

import com.zenith.mc.RegistryData;

public record ItemData(
    int id,
    String name,
    int stackSize
) implements RegistryData { }
