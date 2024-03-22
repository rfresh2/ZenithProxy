package com.zenith.feature.pathing.raycast;

import com.zenith.feature.pathing.World;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;

import static com.zenith.Shared.*;

public class RaycastHelper {

    public static BlockRaycastResult playerRaycastBlock(double maxDistance, boolean includeFluids) {
        return raycastBlock(CACHE.getPlayerCache().getX(), MODULE.get(PlayerSimulation.class).getEyeY(), CACHE.getPlayerCache().getZ(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch(), maxDistance, includeFluids);
    }

    public static BlockRaycastResult raycastBlock(double x, double y, double z, double yaw, double pitch, double maxDistance, boolean includeFluids) {
        final Vector3d viewVec = MathHelper.calculateViewVector(yaw, pitch);

        // end point of the ray
        final double targetX = x + (viewVec.getX() * maxDistance);
        final double targetY = y + (viewVec.getY() * maxDistance);
        final double targetZ = z + (viewVec.getZ() * maxDistance);
        DEFAULT_LOG.info("Raycast from " + x + ", " + y + ", " + z + " to " + targetX + ", " + targetY + ", " + targetZ);

        return raycast(x, y, z, targetX, targetY, targetZ, includeFluids);
    }

    // TODO: calculate block face direction based on collision box

    public static BlockRaycastResult raycast(double x1, double y1, double z1, // start point
                                              double x2, double y2, double z2, // end point
                                              boolean includeFluids) {
        final double startX = MathHelper.lerp(-1.0E-7, x1, x2);
        final double startY = MathHelper.lerp(-1.0E-7, y1, y2);
        final double startZ = MathHelper.lerp(-1.0E-7, z1, z2);
        final double endX = MathHelper.lerp(-1.0E-7, x2, x1);
        final double endY = MathHelper.lerp(-1.0E-7, y2, y1);
        final double endZ = MathHelper.lerp(-1.0E-7, z2, z1);

        int resX = MathHelper.floorToInt(startX);
        int resY = MathHelper.floorToInt(startY);
        int resZ = MathHelper.floorToInt(startZ);
        Block block = checkRaycastResult(resX, resY, resZ, includeFluids);
        if (!block.equals(Block.AIR)) {
            return new BlockRaycastResult(true, resX, resY, resZ, block);
        }

        final double dx = endX - startX;
        final double dy = endY - startY;
        final double dz = endZ - startZ;
        final int dxSign = MathHelper.sign(dx);
        final int dySign = MathHelper.sign(dy);
        final int dzSign = MathHelper.sign(dz);
        final double xStep = dxSign == 0 ? Double.MAX_VALUE : dxSign / dx;
        final double yStep = dySign == 0 ? Double.MAX_VALUE : dySign / dy;
        final double zStep = dzSign == 0 ? Double.MAX_VALUE : dzSign / dz;
        double xFrac = xStep * (dxSign > 0 ? 1.0 - MathHelper.frac(startX) : MathHelper.frac(startX));
        double yFrac = yStep * (dySign > 0 ? 1.0 - MathHelper.frac(startY) : MathHelper.frac(startY));
        double zFrac = zStep * (dzSign > 0 ? 1.0 - MathHelper.frac(startZ) : MathHelper.frac(startZ));

        while (xFrac <= 1.0 || yFrac <= 1.0 || zFrac <= 1.0) {
            if (xFrac < yFrac) {
                if (xFrac < zFrac) {
                    resX += dxSign;
                    xFrac += xStep;
                } else {
                    resZ += dzSign;
                    zFrac += zStep;
                }
            } else if (yFrac < zFrac) {
                resY += dySign;
                yFrac += yStep;
            } else {
                resZ += dzSign;
                zFrac += zStep;
            }

            block = checkRaycastResult(resX, resY, resZ, includeFluids);
            if (!block.equals(Block.AIR)) {
                return new BlockRaycastResult(true, resX, resY, resZ, block);
            }
        }

        return new BlockRaycastResult(false, 0, 0, 0, Block.AIR);
    }

    private static Block checkRaycastResult(final int x, final int y, final int z, final boolean includeFluids) {
        var block = World.getBlockAtBlockPos(x, y, z);
        if (!includeFluids && World.isWater(block)) {
            return Block.AIR;
        } else {
            return block;
        }
    }
}
