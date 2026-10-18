package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.feature.pathfinder.PlayerContext;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import com.zenith.util.struct.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BuildLimitPathFinder implements IElytraPathFinder {
    final int flightLevel;
    final PlayerContext playerCtx;
    final NetherPathfinderContext netherCtx;

    public BuildLimitPathFinder(PlayerContext ctx, NetherPathfinderContext netherCtx) {
        if (ctx == null) {
            throw new IllegalArgumentException("PlayerContext cannot be null");
        }
        this.playerCtx = ctx;

        if (netherCtx == null) {
            throw new IllegalArgumentException("NetherPathfinderContext cannot be null");
        }

        this.flightLevel = World.getCurrentDimension().buildHeight() + 16;
        this.netherCtx = netherCtx;

        if(netherCtx.getMaxHeight() + World.getCurrentDimension().minY() < World.getCurrentDimension().buildHeight()) {
            throw new IllegalStateException("Nether pathfinder max height is below world build limit, cannot proceed");
        }
    }

    /**
     * Generates a direct path from the start to the destination at a fixed y-level above build limit
     * @param start
     * @param destination
     * @param bufferDistance Distance from the destination to halt the direct path
     * @param maxPathSize Maximum number of nodes in the returned path
     * @return A tuple containing the path as a list of BlockPos and a boolean indicating if the path is complete
     */
    public Pair<List<BlockPos>, Boolean> generateDirectPath(BlockPos start, BlockPos destination, int bufferDistance, int maxPathSize) {
        final LinkedList<BlockPos> path = new LinkedList<>();
        final int stepDistance = 32;

        var startFixed = start.y() == flightLevel ? start : new BlockPos(start.x(), flightLevel, start.z());
        var destinationFixed = destination.y() == flightLevel ? destination : new BlockPos(destination.x(), flightLevel, destination.z());

        var cur = startFixed;
        path.add(cur);

        while (path.size() < maxPathSize) {
            double deltaX = destinationFixed.x() - cur.x();
            double deltaZ = destinationFixed.z() - cur.z();
            double remainingDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double remainingDistanceSq = deltaX * deltaX + deltaZ * deltaZ;

            if(remainingDistanceSq <= bufferDistance * bufferDistance) {
                // We are within the buffer distance, so we can stop here
                return new Pair<>(path, true);
            } else if (remainingDistance <= stepDistance) {
                path.add(destinationFixed);
                return new Pair<>(path, true);
            }

            double stepRatio = stepDistance / remainingDistance;
            int nextX = (int) Math.round(cur.x() + deltaX * stepRatio);
            int nextZ = (int) Math.round(cur.z() + deltaZ * stepRatio);

            cur = new BlockPos(nextX, flightLevel, nextZ);
            path.add(cur);
        }

        return new Pair<>(path, false);
    }

    /**
     * Attempts to find an open spot in the sky to transition up above the build limit so a simple direct path can be followed
     * @param start
     * @param destination
     * @return A tuple containing the path that transitions above build limit and a boolean indicating if a transition was found
     */
    public Pair<List<BlockPos>,Boolean> generateTransitionUp(BlockPos start, BlockPos destination) {
        final double deltaX = destination.x() - start.x();
        final double deltaZ = destination.z() - start.z();
        final double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        final double scale = distance == 0 ? 0 : 8 / distance;
        final double stepX = deltaX * scale;
        final double stepZ = deltaZ * scale;

        final int netherMaxHeight = netherCtx.getMaxHeight() + World.getCurrentDimension().minY() - 1;

        var startChunk = new BlockPos((start.x() >> 4) << 4, 0, (start.z() >> 4) << 4);

        if(!isSkyClear(startChunk, start.y())) {
            return new Pair<>(new LinkedList<>(), false);
        }

        LinkedList<BlockPos> path = new LinkedList<>();

        // Start with the middle block so the transition doesn't leave the only chunk we can confirm is clear
        var middlePos = startChunk.offset(8, netherMaxHeight + 4, 8);

        for(int i = 2; i <= 2; i++) {
            var next = new BlockPos(
                    (int) (middlePos.x() + (stepX * i)),
                    netherMaxHeight + (i * 8),
                    (int) (middlePos.z() + (stepZ * i))
            );
            path.add(next);
        }

        return new Pair<>(path, true);
    }

    /**
     * Attempts to find an open spot in the sky to transition down to a flight level the nether pathfinder can navigiate at.
     * @param start
     * @return A tuple containing the path (single point) and a boolean indicating if a transition point was found
     */
    public Pair<List<BlockPos>,Boolean> generateTransitionDown(BlockPos start) {
        final int netherMaxHeight = netherCtx.getMaxHeight() + World.getCurrentDimension().minY() - 1;
        var startChunk = new BlockPos((start.x() >> 4) << 4, 0, (start.z() >> 4) << 4);

        LinkedList<BlockPos> path = new LinkedList<>();

        if(!isSkyClear(new BlockPos((start.x() >> 4) << 4, 0, (start.z() >> 4) << 4), netherMaxHeight-16)) {
            return new Pair<>(new LinkedList<>(), false);
        }

        path.add(new BlockPos(startChunk.offset(8, netherMaxHeight - 8, 8)));
        return new Pair<>(path, true);
    }

    public boolean isSkyClear(BlockPos pos, int y) {
        if(!World.isChunkLoadedBlockPos(pos.x(), pos.z())) {
            return false;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var blockPos = pos.offset(x, y, z);
                int height = World.getSurfaceHeight(blockPos.x(), blockPos.z());
                if (height > y) {
                    return false;
                }
            }
        }
        return true;
    }


    public CompletableFuture<UnpackedSegment> pathFindAsync(BlockPos src, BlockPos dst) {
        final int netherMaxHeight = netherCtx.getMaxHeight() + World.getCurrentDimension().minY() - 1;
        final int maxDirectPathSize = 500;

        // There can be some navigation issues around failed transitions if the threshold distance isn't large enough
        final double maxDistance = 500 /* Baritone.settings().elytraLongDistanceThreshold */ >= 32 ? (double) 500 /* Baritone.settings().elytraLongDistanceThreshold */ : Double.POSITIVE_INFINITY;

        final double distanceXZ = src.squaredDistance(new BlockPos(dst.x(), src.y(), dst.z()));
        final boolean isLongDistance = distanceXZ > maxDistance * maxDistance;
        final boolean srcAboveSupportedHeight = src.y() >= netherMaxHeight;
        final boolean dstAboveSupportedHeight = dst.y() >= netherMaxHeight;

        if(srcAboveSupportedHeight && dstAboveSupportedHeight) {
            var path = generateDirectPath(new BlockPos(src), new BlockPos(dst), 0, maxDirectPathSize);
            return CompletableFuture.completedFuture(new UnpackedSegment(path.left().stream(), path.right()));
        }

        if(isLongDistance) {
            if(srcAboveSupportedHeight) {
                var directPath = generateDirectPath(new BlockPos(src), new BlockPos(dst), (int)maxDistance, maxDirectPathSize);
                return CompletableFuture.completedFuture(
                        new UnpackedSegment(
                                directPath.left().stream(),
                                dstAboveSupportedHeight ? directPath.right() : false
                        )
                );
            } else {
                var transition = generateTransitionUp(new BlockPos(src), new BlockPos(dst));
                var path = transition.left();
                var success = transition.right();

                if(success) {
                    var directPath = generateDirectPath(path.get(path.size()-1), new BlockPos(dst), (int)maxDistance, maxDirectPathSize);
                    path.addAll(directPath.left());

                    return CompletableFuture.completedFuture(
                        new UnpackedSegment(
                            path.stream(),
                            dstAboveSupportedHeight? directPath.right() : false
                        )
                    );
                }
            }

            // Failed to find a transition point so navigate a bit in the right direction and try
            final double deltaX = dst.x() - src.x();
            final double deltaZ = dst.z() - src.z();
            final double scale = (maxDistance/2) / Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            final double stepX = deltaX * scale;
            final double stepZ = deltaZ * scale;
            var midDst = new BlockPos((int)(src.x() + stepX), netherMaxHeight, (int)(src.z() + stepZ));

            return incompletePathfind(src, midDst);
        } else {
            if(srcAboveSupportedHeight) {
                var transition = generateTransitionDown(new BlockPos(src));
                List<BlockPos> path = transition.left();
                boolean success = transition.right();

                if(!success) {
                    var newDest = distanceXZ > 32 ? new BlockPos(dst) : new BlockPos(dst.x(), World.getCurrentDimension().buildHeight(), dst.z());
                    var directPath = generateDirectPath(new BlockPos(src), newDest, 0, 2);
                    return CompletableFuture.completedFuture(new UnpackedSegment(directPath.left().stream(), directPath.right()));
                }

                return CompletableFuture.supplyAsync(() -> {
                    var np = blockingPathFind(path.get(path.size() - 1), dst);
                    path.addAll(np.collect());
                    return new UnpackedSegment(path.stream(), np.isFinished());
                });
            }


            if(dstAboveSupportedHeight) {
                var transition = generateTransitionUp(new BlockPos(src), new BlockPos(dst));
                var path = transition.left();
                var success = transition.right();

                if(success) {
                    var directPath = generateDirectPath(path.get(path.size() - 1), new BlockPos(dst), 0, maxDirectPathSize);
                    path.addAll(directPath.left());
                    return CompletableFuture.completedFuture(new UnpackedSegment(path.stream(), directPath.right()));
                }

                return netherCtx.pathFindAsync(src, new BlockPos(dst.x(), netherMaxHeight, dst.z()));
            }

            return netherCtx.pathFindAsync(src, dst);
        }
    }

    /**
     * A wrapper for a nether pathfinder call but the returned path will always indicate it is incomplete
     * @param src
     * @param dst
     * @return a CompletableFuture containing an UnpackedSegment with isFinished always false
     */
    private CompletableFuture<UnpackedSegment> incompletePathfind(BlockPos src, BlockPos dst) {
        return CompletableFuture.supplyAsync(() -> {
            var packed = blockingPathFind(src, dst);
            return new UnpackedSegment(
                    packed.collect().stream(),
                    false
            );
        });
    }

    private UnpackedSegment blockingPathFind(BlockPos src, BlockPos dst) {
        try {
            return netherCtx.pathFindAsync(src, dst).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof PathCalculationException) {
                throw (PathCalculationException) cause;
            }
            throw new RuntimeException(e);
        }
    }

}
