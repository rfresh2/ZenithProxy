package com.zenith.feature.world.blockdata;

public record Block(int id, String name, boolean isBlock, int minStateId, int maxStateId, int mapColorId) {
    public static Block AIR = new Block(0, "air", false, 0, 0, 0);
}
