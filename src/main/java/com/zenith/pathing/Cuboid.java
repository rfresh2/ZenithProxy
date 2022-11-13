package com.zenith.pathing;

import lombok.Data;

@Data
public class Cuboid {
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;

    public static boolean playerIntersectsWithBlock(final Position playerPosition, final BlockPos blockPos) {
        return playerPosToCuboid(playerPosition).intersects(blockPosToCuboid(blockPos));
    }

    public static Cuboid playerPosToCuboid(final Position playerPosition) {
        // current player coordinates are in the middle (xz) of their bounding box
        final double halfW = 0.3;
        return new Cuboid(
                playerPosition.getX() - halfW, playerPosition.getX() + halfW,
                playerPosition.getY(), playerPosition.getY() + 1.8,
                playerPosition.getZ() - halfW, playerPosition.getZ() + halfW);
    }

    public static Cuboid blockPosToCuboid(final BlockPos blockPos) {
        // blockPos is at lower northwest (-y -z -x corner)
        return new Cuboid(
                blockPos.getX(), blockPos.getX() + 1,
                blockPos.getY(), blockPos.getY() + 1,
                blockPos.getZ(), blockPos.getZ() + 1
        );
    }

    public boolean intersects(final Cuboid cuboid) {
        return this.maxX >= cuboid.minX && this.minX <= cuboid.maxX
                && this.maxZ >= cuboid.minZ && this.minZ <= cuboid.maxZ
                && this.maxY >= cuboid.minY && this.minY <= cuboid.maxY;
    }
}


