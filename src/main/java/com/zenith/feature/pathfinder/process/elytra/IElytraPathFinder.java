package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.mc.block.BlockPos;

import java.util.concurrent.CompletableFuture;

public interface IElytraPathFinder {
    CompletableFuture<UnpackedSegment> pathFindAsync(final BlockPos src, final BlockPos dst);
}
