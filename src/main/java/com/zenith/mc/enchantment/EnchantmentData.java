package com.zenith.mc.enchantment;

import com.zenith.mc.RegistryData;

public record EnchantmentData(
    int id,
    String name
) implements RegistryData { }
