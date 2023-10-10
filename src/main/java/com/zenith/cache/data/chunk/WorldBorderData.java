package com.zenith.cache.data.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorldBorderData {
    private double centerX;
    private double centerZ;
    private double size;
    private int portalTeleportBoundary;
    private int warningBlocks;
    private int warningTime;

    public static final WorldBorderData DEFAULT = new WorldBorderData(0.0, 0.0, 5.9999968E7, 29999984, 5, 15);
}
