package com.zenith.feature.player;

import com.zenith.cache.data.entity.Entity;
import org.cloudburstmc.math.vector.Vector2f;

import static com.zenith.Globals.CACHE;

public final class RotationHelper {
    private RotationHelper() {}

    public static float yawToXZ(final double x, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return (float) yaw;
    }

    public static Vector2f rotationTo(final double x, final double y, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dy = y - CACHE.getPlayerCache().getEyeY();
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        final double pitch = -Math.toDegrees(Math.atan2(dy, distance));
        return Vector2f.from((float) yaw, (float) pitch);
    }

    public static Vector2f shortestRotationTo(final Entity entity) {
        // find the nearest point on the entity CB to the player eyes
        final double playerX = CACHE.getPlayerCache().getX();
        final double playerY = CACHE.getPlayerCache().getEyeY();
        final double playerZ = CACHE.getPlayerCache().getZ();
        var dimensions = entity.dimensions();
        final double entityHeight = dimensions.getY();
        final double entityWidth = dimensions.getX();
        final double halfW = entityWidth / 2.0;
        final double nearestX = Math.clamp(playerX, entity.getX() - halfW, entity.getX() + halfW);
        final double nearestY = Math.clamp(playerY, entity.getY(), entity.getY() + entityHeight);
        final double nearestZ = Math.clamp(playerZ, entity.getZ() - halfW, entity.getZ() + halfW);
        return rotationTo(nearestX, nearestY, nearestZ);
    }

    // assumes cubic block shape
    public static Vector2f shortestRotationTo(final int blockX, final int blockY, final int blockZ) {
        final double playerX = CACHE.getPlayerCache().getX();
        final double playerY = CACHE.getPlayerCache().getEyeY();
        final double playerZ = CACHE.getPlayerCache().getZ();
        final double nearestX = Math.clamp(playerX, blockX, blockX + 1);
        final double nearestY = Math.clamp(playerY, blockY, blockY + 1);
        final double nearestZ = Math.clamp(playerZ, blockZ, blockZ + 1);
        return rotationTo(nearestX, nearestY, nearestZ);
    }
}
