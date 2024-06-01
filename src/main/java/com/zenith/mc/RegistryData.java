package com.zenith.mc;

public interface RegistryData {
    int id();
    // equivalent to resource key (minus the 'minecraft:' prefix
    String name();
}
