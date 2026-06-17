package com.zenith.mc.block.properties;

import com.zenith.mc.block.properties.api.StringRepresentable;

public enum PotentSulfurState implements StringRepresentable {
    DRY("dry"),
    WET("wet"),
    DORMANT("dormant"),
    ERUPTING("erupting"),
    CONTINUOUS("continuous");

    private final String name;

    PotentSulfurState(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
