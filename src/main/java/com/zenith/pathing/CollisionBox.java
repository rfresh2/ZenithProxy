package com.zenith.pathing;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CollisionBox {
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;

    public static boolean playerIntersectsWithBlock(final Position playerPosition, final BlockPos blockPos) {
        return playerPosToCollisionBox(playerPosition).intersects(blockPosToCollisionBox(blockPos));
    }

    public static boolean playerIntersectsWithGenericCollisionBoxes(final Position playerPosition, final BlockPos blockPos, final List<CollisionBox> genericCollisionBoxes) {
        final CollisionBox playerCollisionBox = playerPosToCollisionBox(playerPosition);
        final List<CollisionBox> localizedCollisionBoxes = genericCollisionBoxes.stream()
                .map(genericCollisionBox -> blockPosAndGenericCollisionBoxToLocalizedCollisionBox(blockPos, genericCollisionBox))
                .collect(Collectors.toList());
        for (final CollisionBox blockCollisionBox : localizedCollisionBoxes) {
            if (playerCollisionBox.intersects(blockCollisionBox)) {
                return true;
            }
        }
        return false;
    }

    public static CollisionBox playerPosToCollisionBox(final Position playerPosition) {
        // current player coordinates are in the middle (xz) of their bounding box
        final double halfW = 0.3;
        return new CollisionBox(
                playerPosition.getX() - halfW, playerPosition.getX() + halfW,
                playerPosition.getY(), playerPosition.getY() + 1.8,
                playerPosition.getZ() - halfW, playerPosition.getZ() + halfW);
    }

    public static CollisionBox blockPosToCollisionBox(final BlockPos blockPos) {
        // blockPos is at lower northwest (-y -z -x corner)
        return new CollisionBox(
                blockPos.getX(), blockPos.getX() + 1,
                blockPos.getY(), blockPos.getY() + 1,
                blockPos.getZ(), blockPos.getZ() + 1
        );
    }

    public static CollisionBox blockPosAndGenericCollisionBoxToLocalizedCollisionBox(final BlockPos blockPos, final CollisionBox genericCollisionBox) {
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
}


