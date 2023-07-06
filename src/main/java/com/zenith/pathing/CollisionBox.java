package com.zenith.pathing;

import lombok.Data;

import java.util.List;

@Data
public class CollisionBox {
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;

    /**
     * MC Data has "generic" CollisionBoxes - they aren't localized to a BlockPos.
     * To see if we intersect we need to check at the actual world's coords and localize both
     * the player and the block's CollisionBox's to those coords.
     */
    public static boolean playerIntersectsWithGenericCollisionBoxes(final Position playerPosition, final BlockPos blockPos, final List<CollisionBox> genericCollisionBoxes) {
        for(final CollisionBox genericCollisionBox : genericCollisionBoxes) {
            if (genericCollisionBox.intersectsAtLocalizedPosWithPlayerPosition(playerPosition, blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
                return true;
            }
        }
        return false;
    }

    private static CollisionBox playerPosToCollisionBox(final Position playerPosition) {
        // current player coordinates are in the middle (xz) of their bounding box
        final double halfW = 0.3;
        return new CollisionBox(
                playerPosition.getX() - halfW, playerPosition.getX() + halfW,
                playerPosition.getY(), playerPosition.getY() + 1.8,
                playerPosition.getZ() - halfW, playerPosition.getZ() + halfW);
    }

    private static CollisionBox blockPosToCollisionBox(final BlockPos blockPos) {
        // blockPos is at lower northwest (-y -z -x corner)
        return new CollisionBox(
                blockPos.getX(), blockPos.getX() + 1,
                blockPos.getY(), blockPos.getY() + 1,
                blockPos.getZ(), blockPos.getZ() + 1
        );
    }

    private static CollisionBox blockPosAndGenericCollisionBoxToLocalizedCollisionBox(final BlockPos blockPos, final CollisionBox genericCollisionBox) {
        return new CollisionBox(
                blockPos.getX() + genericCollisionBox.minX,
                blockPos.getX() + genericCollisionBox.maxX,
                blockPos.getY() + genericCollisionBox.minY,
                blockPos.getY() + genericCollisionBox.maxY,
                blockPos.getZ() + genericCollisionBox.minZ,
                blockPos.getZ() + genericCollisionBox.maxZ
        );
    }

    public boolean intersects(final CollisionBox collisionBox) {
        return this.maxX >= collisionBox.minX && this.minX <= collisionBox.maxX
                && this.maxZ >= collisionBox.minZ && this.minZ <= collisionBox.maxZ
                && this.maxY >= collisionBox.minY && this.minY <= collisionBox.maxY;
    }

    public boolean intersectsAtLocalizedPos(final CollisionBox collisionBox, final int xLocal, final int yLocal, final int zLocal) {
        return this.maxX >= (xLocal + collisionBox.minX) && this.minX <= (xLocal + collisionBox.maxX)
                && this.maxZ >= (zLocal + collisionBox.minZ) && this.minZ <= (zLocal + collisionBox.maxZ)
                && this.maxY >= (yLocal + collisionBox.minY) && this.minY <= (yLocal + collisionBox.maxY);
    }

    public boolean intersectsAtLocalizedPosWithPlayerPosition(final Position playerPosition, final int xLocal, final int yLocal, final int zLocal) {
        return (this.maxX + xLocal) >= (playerPosition.getX() - 0.3) && (this.minX + xLocal) <= (playerPosition.getX() + 0.3)
                && (this.maxZ + zLocal) >= (playerPosition.getZ() - 0.3) && (this.minZ + zLocal) <= (playerPosition.getZ() + 0.3)
                && (this.maxY + yLocal) >= playerPosition.getY() && (this.minY + yLocal) <= (playerPosition.getY() + 1.8);
    }
}


