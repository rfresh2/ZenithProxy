package com.zenith.feature.pathfinder.process.elytra.pathfinder;

import com.zenith.mc.block.BlockPos;

import java.util.List;

public class PathSegment {
    public final boolean finished;
    public final List<BlockPos> blocks;

    public PathSegment(boolean finished, List<BlockPos> blocks) {
        this.finished = finished;
        this.blocks = blocks;
    }
}
