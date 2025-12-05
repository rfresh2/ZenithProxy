package com.zenith.feature.player.raycast;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.CollisionBox;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.mc.entity.EntityData;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.List;

import static com.zenith.Globals.*;

public class RaycastHelper {

    public static BlockRaycastResult playerBlockRaycast(double maxDistance, boolean includeFluids) {
        var sim = BOT;
        return blockRaycastFromPos(sim.getX(), sim.getEyeY(), sim.getZ(), sim.getYaw(), sim.getPitch(), maxDistance, includeFluids);
    }

    public static BlockRaycastResult blockRaycastFromPos(double x, double y, double z, double yaw, double pitch, double maxDistance, boolean includeFluids) {
        var rayEndPos = MathHelper.calculateRayEndPos(x, y, z, yaw, pitch, maxDistance);
        return blockRaycast(x, y, z, rayEndPos.getX(), rayEndPos.getY(), rayEndPos.getZ(), includeFluids);
    }

    public static BlockRaycastResult blockRaycast(double x1, double y1, double z1, // start point
                                                  double x2, double y2, double z2, // end point
                                                  boolean includeFluids) {
        final double startX = MathHelper.lerp(-1.0E-7, x1, x2);
        final double startY = MathHelper.lerp(-1.0E-7, y1, y2);
        final double startZ = MathHelper.lerp(-1.0E-7, z1, z2);
        final double endX = MathHelper.lerp(-1.0E-7, x2, x1);
        final double endY = MathHelper.lerp(-1.0E-7, y2, y1);
        final double endZ = MathHelper.lerp(-1.0E-7, z2, z1);

        int resX = MathHelper.floorI(startX);
        int resY = MathHelper.floorI(startY);
        int resZ = MathHelper.floorI(startZ);

        final int insideBlockState = World.getBlockStateId(resX, resY, resZ);
        Block block = BLOCK_DATA.getBlockDataFromBlockStateId(insideBlockState);
        if (!block.isAir()) {
            var raycastResult = checkBlockRaycast(startX, startY, startZ, endX, endY, endZ, resX, resY, resZ, insideBlockState, block, includeFluids);
            if (raycastResult.hit()) return raycastResult;
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

            final int blockStateId = World.getBlockStateId(resX, resY, resZ);
            block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
            if (!block.isAir()) {
                var raycastResult = checkBlockRaycast(startX, startY, startZ, endX, endY, endZ, resX, resY, resZ, blockStateId, block, includeFluids);
                if (raycastResult.hit()) return raycastResult;
            }
        }

        return BlockRaycastResult.miss();
    }

    public static BlockRaycastResult playerEyeRaycastThroughToBlockTarget(int blockX, int blockY, int blockZ) {
        var sim = BOT;
        return playerEyeRaycastThroughToBlockTarget(blockX, blockY, blockZ, sim.getYaw(), sim.getPitch(), sim.getBlockReachDistance());
    }

    public static BlockRaycastResult playerEyeRaycastThroughToBlockTarget(int blockX, int blockY, int blockZ, float yaw, float pitch) {
        return playerEyeRaycastThroughToBlockTarget(blockX, blockY, blockZ, yaw, pitch, BOT.getBlockReachDistance());
    }

    public static BlockRaycastResult playerEyeRaycastThroughToBlockTarget(int blockX, int blockY, int blockZ, float yaw, float pitch, double blockReachDistance) {
        var sim = BOT;
        final double x1 = sim.getX();
        final double y1 = sim.getEyeY();
        final double z1 = sim.getZ();
        var rayEndPos = MathHelper.calculateRayEndPos(x1, y1, z1, yaw, pitch, blockReachDistance);
        final double startX = MathHelper.lerp(-1.0E-7, x1, rayEndPos.getX());
        final double startY = MathHelper.lerp(-1.0E-7, y1, rayEndPos.getY());
        final double startZ = MathHelper.lerp(-1.0E-7, z1, rayEndPos.getZ());
        final double endX = MathHelper.lerp(-1.0E-7, rayEndPos.getX(), x1);
        final double endY = MathHelper.lerp(-1.0E-7, rayEndPos.getY(), y1);
        final double endZ = MathHelper.lerp(-1.0E-7, rayEndPos.getZ(), z1);
        final int blockStateId = World.getBlockStateId(blockX, blockY, blockZ);
        Block block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
        if (block == null || block.isAir()) return BlockRaycastResult.miss();
        final List<CollisionBox> collisionBoxes = BLOCK_DATA.getInteractionBoxesFromBlockStateId(blockStateId);
        if (collisionBoxes == null || collisionBoxes.isEmpty()) return BlockRaycastResult.miss();
        BlockRaycastResult result = BlockRaycastResult.miss();
        double prevLen = Double.MAX_VALUE;
        List<LocalizedCollisionBox> localizedCollisionBoxes = BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, blockX, blockY, blockZ);
        for (int i = 0; i < localizedCollisionBoxes.size(); i++) {
            final LocalizedCollisionBox cb = localizedCollisionBoxes.get(i);
            final RayIntersection intersection = cb.rayIntersection(startX, startY, startZ, endX, endY, endZ);
            if (intersection == null) continue;
            final double thisLen = MathHelper.squareLen(intersection.x(), intersection.y(), intersection.z());
            if (thisLen < prevLen) {
                result = new BlockRaycastResult(true, blockX, blockY, blockZ, intersection, block);
                prevLen = thisLen;
            }
        }
        return result;
    }

    public static EntityRaycastResult playerEntityRaycast(double maxDistance) {
        var sim = BOT;
        return entityRaycastFromPos(sim.getX(), sim.getEyeY(), sim.getZ(), sim.getYaw(), sim.getPitch(), maxDistance);
    }

    public static EntityRaycastResult entityRaycastFromPos(final double x, final double y, final double z, final float yaw, final float pitch, final double maxDistance) {
        final Vector3d rayEndPos = MathHelper.calculateRayEndPos(x, y, z, yaw, pitch, maxDistance);
        return entityRaycast(x, y, z, rayEndPos.getX(), rayEndPos.getY(), rayEndPos.getZ());
    }

    private static EntityRaycastResult entityRaycast(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double startX = MathHelper.lerp(-1.0E-7, x1, x2);
        final double startY = MathHelper.lerp(-1.0E-7, y1, y2);
        final double startZ = MathHelper.lerp(-1.0E-7, z1, z2);
        final double endX = MathHelper.lerp(-1.0E-7, x2, x1);
        final double endY = MathHelper.lerp(-1.0E-7, y2, y1);
        final double endZ = MathHelper.lerp(-1.0E-7, z2, z1);

        final double rayLength = MathHelper.distanceSq3d(x1, y1, z1, x2, y2, z2);
        EntityRaycastResult resultRaycast = EntityRaycastResult.miss();
        double resultRaycastDistanceToStart = Double.MAX_VALUE;

        for (Entity e : CACHE.getEntityCache().getEntities().values()) {
            if (e instanceof EntityPlayer p && p.isSelfPlayer()) continue;
            // filter out entities that are too far away to possibly intersect
            // add 10 block leniency to account for the entity position not calculating the interaction box
            // we can avoid a few object heap allocations for the localized cb's by doing this
            if (rayLength + 100 <= MathHelper.distanceSq3d(x1, y1, z1, e.getX(), e.getY(), e.getZ())) continue;
            EntityData data = ENTITY_DATA.getEntityData(e.getEntityType());
            if (data == null) continue;
            if (!data.pickable()) continue;
            LocalizedCollisionBox cb = entityCollisionBox(e);
            RayIntersection intersection = cb.rayIntersection(startX, startY, startZ, endX, endY, endZ);
            if (intersection != null) {
                double intersectingRayLen = MathHelper.distanceSq3d(startX, startY, startZ, intersection.x(), intersection.y(), intersection.z());
                if (intersectingRayLen < resultRaycastDistanceToStart) {
                    resultRaycastDistanceToStart = intersectingRayLen;
                    resultRaycast = new EntityRaycastResult(true, intersection, e);
                }
            }
        }
        return resultRaycast;
    }

    // ignoring all intersections with other blocks and entities
    public static EntityRaycastResult playerEyeRaycastThroughToTarget(Entity target, double entityReachDistance) {
        var sim = BOT;
        return playerEyeRaycastThroughToTarget(target, sim.getYaw(), sim.getPitch(), entityReachDistance);
    }

    public static EntityRaycastResult playerEyeRaycastThroughToTarget(Entity target) {
        return playerEyeRaycastThroughToTarget(target, BOT.getEntityInteractDistance());
    }

    public static EntityRaycastResult playerEyeRaycastThroughToTarget(Entity target, float yaw, float pitch, double entityReachDistance) {
        var sim = BOT;
        final double x1 = sim.getX();
        final double y1 = sim.getEyeY();
        final double z1 = sim.getZ();
        var rayEndPos = MathHelper.calculateRayEndPos(x1, y1, z1, yaw, pitch, entityReachDistance);
        final double startX = MathHelper.lerp(-1.0E-7, x1, rayEndPos.getX());
        final double startY = MathHelper.lerp(-1.0E-7, y1, rayEndPos.getY());
        final double startZ = MathHelper.lerp(-1.0E-7, z1, rayEndPos.getZ());
        final double endX = MathHelper.lerp(-1.0E-7, rayEndPos.getX(), x1);
        final double endY = MathHelper.lerp(-1.0E-7, rayEndPos.getY(), y1);
        final double endZ = MathHelper.lerp(-1.0E-7, rayEndPos.getZ(), z1);
        EntityRaycastResult resultRaycast = EntityRaycastResult.miss();
        EntityData data = ENTITY_DATA.getEntityData(target.getEntityType());
        if (data == null) return resultRaycast;
        LocalizedCollisionBox cb = entityCollisionBox(target);
        RayIntersection intersection = cb.rayIntersection(startX, startY, startZ, endX, endY, endZ);
        if (intersection != null) {
            resultRaycast = new EntityRaycastResult(true, intersection, target);
        }
        return resultRaycast;
    }

    public static EntityRaycastResult playerEyeRaycastThroughToTarget(Entity target, float yaw, float pitch) {
        return playerEyeRaycastThroughToTarget(target, yaw, pitch, BOT.getEntityInteractDistance());
    }

    private static LocalizedCollisionBox entityCollisionBox(final Entity entity) {
        var dimensions = entity.dimensions();
        double width = dimensions.getX();
        double height = dimensions.getY();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double minX = x - width / 2;
        double maxX = x + width / 2;
        double minY = y;
        double maxY = y + height;
        double minZ = z - width / 2;
        double maxZ = z + width / 2;
        return new LocalizedCollisionBox(minX, maxX, minY, maxY, minZ, maxZ, x, y, z);
    }

    private static BlockRaycastResult checkBlockRaycast(
        double x, double y, double z,
        double x2, double y2, double z2,
        int blockX, int blockY, int blockZ,
        int blockStateId,
        Block block,
        boolean includeFluids) {
        if (!includeFluids && World.isWater(block)) {
            return new BlockRaycastResult(false, 0, 0, 0, null, BlockRegistry.AIR);
        }
        final List<CollisionBox> collisionBoxes = BLOCK_DATA.getInteractionBoxesFromBlockStateId(blockStateId);
        if (collisionBoxes == null || collisionBoxes.isEmpty()) return BlockRaycastResult.miss();

        BlockRaycastResult result = BlockRaycastResult.miss();
        double prevLen = Double.MAX_VALUE;

        List<LocalizedCollisionBox> localizedCollisionBoxes = BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, blockX, blockY, blockZ);

        for (int i = 0; i < localizedCollisionBoxes.size(); i++) {
            final LocalizedCollisionBox cb = localizedCollisionBoxes.get(i);
            final RayIntersection intersection = cb.rayIntersection(x, y, z, x2, y2, z2);
            if (intersection == null) continue;
            final double thisLen = MathHelper.squareLen(intersection.x(), intersection.y(), intersection.z());
            if (thisLen < prevLen) {
                result = new BlockRaycastResult(true, blockX, blockY, blockZ, intersection, block);
                prevLen = thisLen;
            }
        }

        return result;
    }

    public static BlockOrEntityRaycastResult playerBlockOrEntityRaycast(double blockReachDistance, double entityReachDistance) {
        var sim = BOT;
        return blockOrEntityRaycastFromPos(sim.getX(), sim.getEyeY(), sim.getZ(), sim.getYaw(), sim.getPitch(), blockReachDistance, entityReachDistance);
    }

    public static BlockOrEntityRaycastResult blockOrEntityRaycastFromPos(final double x, final double y, final double z, final float yaw, final float pitch, final double blockReachDistance, final double entityReachDistance) {
        final Vector3d blockRayEndPos = MathHelper.calculateRayEndPos(x, y, z, yaw, pitch, blockReachDistance);
        final Vector3d entityRayEndPos = MathHelper.calculateRayEndPos(x, y, z, yaw, pitch, entityReachDistance);
        return blockOrEntityRaycast(x, y, z, blockRayEndPos, entityRayEndPos);
    }

    private static BlockOrEntityRaycastResult blockOrEntityRaycast(final double x, final double y, final double z, Vector3d blockRayEndPos, Vector3d entityRayEndPos) {
        final BlockRaycastResult blockRaycastResult = blockRaycast(x, y, z, blockRayEndPos.getX(), blockRayEndPos.getY(), blockRayEndPos.getZ(), false);
        final EntityRaycastResult entityRaycastResult = entityRaycast(x, y, z, entityRayEndPos.getX(), entityRayEndPos.getY(), entityRayEndPos.getZ());
        // if both hit, return the one that is closer to the start point
        if (blockRaycastResult.hit() && entityRaycastResult.hit()) {
            final double blockDist = MathHelper.distanceSq3d(x, y, z, blockRaycastResult.intersection().x(), blockRaycastResult.intersection().y(), blockRaycastResult.intersection().z());
            final double entityDist = MathHelper.distanceSq3d(x, y, z, entityRaycastResult.intersection().x(), entityRaycastResult.intersection().y(), entityRaycastResult.intersection().z());
            if (blockDist < entityDist) {
                return BlockOrEntityRaycastResult.wrap(blockRaycastResult);
            } else {
                return BlockOrEntityRaycastResult.wrap(entityRaycastResult);
            }
        } else if (blockRaycastResult.hit()) {
            return BlockOrEntityRaycastResult.wrap(blockRaycastResult);
        } else if (entityRaycastResult.hit()) {
            return BlockOrEntityRaycastResult.wrap(entityRaycastResult);
        }
        return BlockOrEntityRaycastResult.miss();
    }
}
