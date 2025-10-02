package com.zenith.mc.block.properties;

import com.zenith.mc.block.properties.api.StringRepresentable;

public enum CopperGolemStatueBlockPose implements StringRepresentable {
    STANDING("standing"),
    SITTING("sitting"),
    RUNNING("running"),
    STAR("star");

    private final String name;

    CopperGolemStatueBlockPose(final String string2) {
        this.name = string2;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
