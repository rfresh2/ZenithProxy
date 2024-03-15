package com.zenith.feature.map;

public enum Brightness {
    LOW(0, 180),
    NORMAL(1, 220),
    HIGH(2, 255),
    LOWEST(3, 135);

    private static final Brightness[] VALUES = new Brightness[]{LOW, NORMAL, HIGH, LOWEST};
    public final int id;
    public final int modifier;

    Brightness(int j, int k) {
        this.id = j;
        this.modifier = k;
    }

    static Brightness byId(int i) {
        return VALUES[i];
    }
}
