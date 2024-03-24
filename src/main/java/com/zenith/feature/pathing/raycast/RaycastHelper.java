package com.zenith.feature.pathing.raycast;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.feature.entities.EntityData;
import com.zenith.feature.pathing.CollisionBox;
import com.zenith.feature.pathing.LocalizedCollisionBox;
import com.zenith.feature.pathing.World;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.*;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;

public class RaycastHelper {

    public static BlockRaycastResult playerBlockRaycast(double maxDistance, boolean includeFluids) {
        return blockRaycastFromPos(CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY() + 1.6, CACHE.getPlayerCache().getZ(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch(), maxDistance, includeFluids);
    }

    public static BlockRaycastResult blockRaycastFromPos(double x, double y, double z, double yaw, double pitch, double maxDistance, boolean includeFluids) {
        final Vector3d viewVec = MathHelper.calculateViewVector(yaw, pitch);

        // end point of the ray
        final double targetX = x + (viewVec.getX() * maxDistance);
        final double targetY = y + (viewVec.getY() * maxDistance);
        final double targetZ = z + (viewVec.getZ() * maxDistance);

        return blockRaycast(x, y, z, targetX, targetY, targetZ, includeFluids);
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
        Block block = getBlockAt(resX, resY, resZ, includeFluids);
        if (!block.equals(Block.AIR)) {
            return new BlockRaycastResult(true, resX, resY, resZ, Direction.DOWN, block);
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
            if (!block.equals(Block.AIR)) {
                var raycastResult = checkBlockRaycast(startX, startY, startZ, endX, endY, endZ, resX, resY, resZ, blockStateId, block, includeFluids);
                if (raycastResult.hit()) return raycastResult;
            }
        }

        return BlockRaycastResult.miss();
    }

    public static EntityRaycastResult playerEntityRaycast(double maxDistance) {
        return entityRaycastFromPos(CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY() + 1.6, CACHE.getPlayerCache().getZ(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch(), maxDistance);
    }

    public static EntityRaycastResult entityRaycastFromPos(final double x, final double y, final double z, final float yaw, final float pitch, final double maxDistance) {
        final Vector3d viewVec = MathHelper.calculateViewVector(yaw, pitch);

        // end point of the ray
        final double targetX = x + (viewVec.getX() * maxDistance);
        final double targetY = y + (viewVec.getY() * maxDistance);
        final double targetZ = z + (viewVec.getZ() * maxDistance);
        return entityRaycast(x, y, z, targetX, targetY, targetZ);
    }

    private static EntityRaycastResult entityRaycast(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double startX = MathHelper.lerp(-1.0E-7, x1, x2);
        final double startY = MathHelper.lerp(-1.0E-7, y1, y2);
        final double startZ = MathHelper.lerp(-1.0E-7, z1, z2);
        final double endX = MathHelper.lerp(-1.0E-7, x2, x1);
        final double endY = MathHelper.lerp(-1.0E-7, y2, y1);
        final double endZ = MathHelper.lerp(-1.0E-7, z2, z1);

        final double rayLength = MathHelper.distance3d(x1, y1, z1, x2, y2, z2);

        // todo: make this more efficient, cut out the maps and use a for loop

        Map<Entity, EntityData> entityToData = CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityStandard || entity instanceof EntityPlayer)
            .filter(entity -> !(entity instanceof EntityPlayer p && p.isSelfPlayer()))
            // filter out entities that are too far away to possibly intersect
            .filter(entity -> rayLength > MathHelper.distance3d(
                x1, y1, z1,
                entity.getX(), entity.getY(), entity.getZ()))
            .collect(Collectors.toMap(
                k -> k,
                v -> ENTITY_DATA.getEntityData(v.getEntityType())
            ));

        final List<EntityRaycastResult> results = new ArrayList<>(1);
        for (Map.Entry<Entity, EntityData> entry : entityToData.entrySet()) {
            Entity entity = entry.getKey();
            EntityData data = entry.getValue();
            if (data == null) continue;
            LocalizedCollisionBox cb = entityCollisionBox(entity, data);
            LocalizedCollisionBox.RayIntersection intersection = cb.rayIntersection(startX, startY, startZ, endX, endY, endZ);
            if (intersection != null) {
                results.add(new EntityRaycastResult(true, entity));
            }
        }
        return results.stream()
            .min(Comparator.comparingDouble(
                e -> MathHelper.distance3d(
                    x1, y1, z1,
                    e.entity().getX(), e.entity().getY(), e.entity().getZ()
                )))
            .orElse(EntityRaycastResult.miss());
    }

    private static LocalizedCollisionBox entityCollisionBox(final Entity entity, final EntityData data) {
        double width = data.width();
        double height = data.height();
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

    private static Block getBlockAt(final int x, final int y, final int z, final boolean includeFluids) {
        var block = World.getBlockAtBlockPos(x, y, z);
        if (!includeFluids && World.isWater(block)) {
            return Block.AIR;
        } else {
            return block;
        }
    }

    // TODO: Does not work for blocks with incongruent interaction boxes
    //   e.g. torches, flowers, etc. Blocks that you don't collide with but can interact with
    private static BlockRaycastResult checkBlockRaycast(
        double x, double y, double z,
        double x2, double y2, double z2,
        int blockX, int blockY, int blockZ,
        int blockStateId,
        Block block,
        boolean includeFluids) {
        if (!includeFluids && World.isWater(block)) {
            return new BlockRaycastResult(false, 0, 0, 0, Direction.UP, Block.AIR);
        }
        final List<CollisionBox> collisionBoxes = BLOCK_DATA.getCollisionBoxesFromBlockStateId(blockStateId);
        if (collisionBoxes == null || collisionBoxes.isEmpty()) return BlockRaycastResult.miss();
        // TODO: improve efficiency
        final List<LocalizedCollisionBox> localizedCBs = collisionBoxes.stream().map(cb -> new LocalizedCollisionBox(cb, blockX, blockY, blockZ)).toList();
        // find intersecting Direction / Block face with the ray (if any)
        final List<LocalizedCollisionBox.RayIntersection> intersections = localizedCBs.stream()
            .map(cb -> cb.rayIntersection(x, y, z, x2, y2, z2))
            .filter(Objects::nonNull)
            .toList();
        if (intersections.isEmpty()) return BlockRaycastResult.miss();
        // select intersection nearest to the start point
        final LocalizedCollisionBox.RayIntersection intersection = intersections.stream()
            .min(Comparator.comparingDouble(a -> MathHelper.squareLen(a.x(), a.y(), a.z())))
            .orElseThrow();
        return new BlockRaycastResult(true, blockX, blockY, blockZ, intersection.intersectingFace(), block);
    }

    public static BlockOrEntityRaycastResult playerBlockOrEntityRaycast(double maxDistance) {
        return blockOrEntityRaycastFromPos(CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY() + 1.6, CACHE.getPlayerCache().getZ(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch(), maxDistance);
    }

    private static BlockOrEntityRaycastResult blockOrEntityRaycastFromPos(final double x, final double y, final double z, final float yaw, final float pitch, final double maxDistance) {
        final Vector3d viewVec = MathHelper.calculateViewVector(yaw, pitch);

        // end point of the ray
        final double targetX = x + (viewVec.getX() * maxDistance);
        final double targetY = y + (viewVec.getY() * maxDistance);
        final double targetZ = z + (viewVec.getZ() * maxDistance);

        return blockOrEntityRaycast(x, y, z, targetX, targetY, targetZ);
    }

    private static BlockOrEntityRaycastResult blockOrEntityRaycast(final double x, final double y, final double z, final double x2, final double y2, final double z2) {
        final BlockRaycastResult blockRaycastResult = blockRaycast(x, y, z, x2, y2, z2, false);
        final EntityRaycastResult entityRaycastResult = entityRaycast(x, y, z, x2, y2, z2);
        // if both hit, return the one that is closer to the start point
        if (blockRaycastResult.hit() && entityRaycastResult.hit()) {
            final double blockDist = MathHelper.distance3d(x, y, z, blockRaycastResult.x(), blockRaycastResult.y(), blockRaycastResult.z());
            final double entityDist = MathHelper.distance3d(x, y, z, entityRaycastResult.entity().getX(), entityRaycastResult.entity().getY(), entityRaycastResult.entity().getZ());
            if (blockDist < entityDist) {
                return new BlockOrEntityRaycastResult(true, blockRaycastResult, null);
            } else {
                return new BlockOrEntityRaycastResult(true, null, entityRaycastResult);
            }
        } else if (blockRaycastResult.hit()) {
            return new BlockOrEntityRaycastResult(true, blockRaycastResult, null);
        } else if (entityRaycastResult.hit()) {
            return new BlockOrEntityRaycastResult(true, null, entityRaycastResult);
        }
        return BlockOrEntityRaycastResult.miss();
    }
}
