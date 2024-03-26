package com.zenith.feature.pathing.blockdata;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMaps;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

public record Block(int id, String name, boolean isBlock, int minStateId, int maxStateId, boolean diggable, double destroySpeed, Int2DoubleMap itemToBreakSpeedMap, IntSet requiredHarvestItems) {
    public static Block AIR = new Block(0, "air", false, 0, 0, false, 0.0, Int2DoubleMaps.EMPTY_MAP, IntSets.emptySet());
}
