package com.zenith.feature.pathing.raycast;

import com.zenith.feature.pathing.blockdata.Block;

public record BlockRaycastResult(boolean hit, double x, double y, double z, Block block) { }
