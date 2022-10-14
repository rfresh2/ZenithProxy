package com.zenith.pathing;

import lombok.RequiredArgsConstructor;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

@RequiredArgsConstructor
public class Pathing {
    private static final double walkBlocksPerTick = 4 / 20.0; // bps / 20
    private final World world;

    // todo: vertical movement
    //  keeping this simple with 2d x-z movement right now
    // todo: this pathing doesn't know how to get around blocks
    //  i.e. sometimes you need to go in the opposite direction to get to a goal
    //  might need to incorporate some algorithm like A* to chart out a full path rather than just the next move
    public Position calculateNextMove(final BlockPos goal) {
        final Position currentPlayerPos = getCurrentPlayerPos();
        final BlockPos currentPlayerBlockPos = currentPlayerPos.toBlockPos();
        final int xDelta = goal.getX() - currentPlayerBlockPos.getX();
        final int zDelta = goal.getZ() - currentPlayerBlockPos.getZ();
        // let's try moving x
        Position xMovePos = currentPlayerPos.addX(walkBlocksPerTick * (double) (xDelta / Math.abs(xDelta)));
        if (isNextWalkSafe(xMovePos)) {
            return xMovePos;
        }
        Position zMovePos = currentPlayerPos.addZ(walkBlocksPerTick * (double) (zDelta / Math.abs(zDelta)));
        if (isNextWalkSafe(zMovePos)) {
            return zMovePos;
        }
        CLIENT_LOG.info("Pathing: No safe movement towards goal found");
        return currentPlayerPos;
    }

    public Position getCurrentPlayerPos() {
        return new Position(CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ());
    }

    public boolean isNextWalkSafe(final Position position) {
        final BlockPos blockPos = position.toBlockPos();
        final boolean groundSolid = this.world.isSolidBlock(blockPos.addY(-1));
        final boolean blocked = this.world.isSolidBlock(blockPos) || this.world.isSolidBlock(blockPos.addY(1));
        return groundSolid && !blocked;
    }
}
