package com.zenith.feature.world;

import com.zenith.mc.block.*;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.experimental.UtilityClass;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

@UtilityClass
public class World {
    @Nullable
    public ChunkSection getChunkSection(final int x, final int y, final int z) {
        try {
            return CACHE.getChunkCache().getChunkSection(x, y, z );
        } catch (final Exception e) {
            CLIENT_LOG.error("error finding chunk section for pos: {}, {}, {}", x, y, z, e);
        }
        return null;
    }

    public int getBlockStateId(final BlockPos blockPos) {
        return getBlockStateId(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public int getBlockStateId(final int x, final int y, final int z) {
        final ChunkSection chunk = getChunkSection(x, y, z);
        if (chunk == null) return 0;
        return chunk.getBlock(x & 15, y & 15, z & 15);
    }

    public BlockState getBlockState(final BlockPos blockPos) {
        return getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public BlockState getBlockState(final long blockPos) {
        return getBlockState(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos));
    }

    public BlockState getBlockState(final int x, final int y, final int z) {
        return new BlockState(getBlockAtBlockPos(x, y, z), getBlockStateId(x, y, z));
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return getBlockAtBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public Block getBlockAtBlockPos(final long blockPos) {
        return getBlockAtBlockPos(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos));
    }

    public Block getBlockAtBlockPos(final int x, final int y, final int z) {
        Block blockData = BLOCK_DATA.getBlockDataFromBlockStateId(getBlockStateId(x, y, z));
        if (blockData == null)
            return BlockRegistry.AIR;
        return blockData;
    }

    public List<LocalizedCollisionBox> getIntersectingCollisionBoxes(final LocalizedCollisionBox cb) {
        final List<LocalizedCollisionBox> boundingBoxList = new ArrayList<>();
        getSolidBlockCollisionBoxes(cb, boundingBoxList);
        getEntityCollisionBoxes(cb, boundingBoxList);
        return boundingBoxList;
    }

    public void getSolidBlockCollisionBoxes(final LocalizedCollisionBox cb, final List<LocalizedCollisionBox> results) {
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var x = BlockPos.getX(blockPos);
            var y = BlockPos.getY(blockPos);
            var z = BlockPos.getZ(blockPos);
            final BlockState blockState = getBlockState(blockPos);
            if (blockState.isSolidBlock()) {
                var collisionBoxes = blockState.getLocalizedCollisionBoxes(x, y, z);
                results.addAll(collisionBoxes);
            }
        }
    }

    public void getEntityCollisionBoxes(final LocalizedCollisionBox cb, final List<LocalizedCollisionBox> results) {
        for (var entity : CACHE.getEntityCache().getEntities().values()) {
            EntityType entityType = entity.getEntityType();
            if (!(entityType == EntityType.BOAT || entityType == EntityType.SHULKER))
                continue;
            if (entity.getPassengerIds().contains(CACHE.getPlayerCache().getThePlayer().getEntityId()))
                continue;
            var entityData = ENTITY_DATA.getEntityData(entityType);
            if (entityData == null) continue;
            var x = entity.getX();
            var y = entity.getY();
            var z = entity.getZ();
            double halfW = entityData.width() / 2.0;
            double minX = x - halfW;
            double minY = y;
            double minZ = z - halfW;
            double maxX = x + halfW;
            double maxY = y + entityData.height();
            double maxZ = z + halfW;
            if (cb.intersects(minX, maxX, minY, maxY, minZ, maxZ)) {
                results.add(new LocalizedCollisionBox(minX, maxX, minY, maxY, minZ, maxZ, x, y, z));
            }
        }
    }

    public boolean isTouchingWater(final LocalizedCollisionBox cb) {
        // adjust collision box slightly to avoid false positives at borders?
        final LocalizedCollisionBox box = cb.stretch(-0.001, -0.001, -0.001);
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
            if (isWater(blockAtBlockPos))
                if (BlockPos.getY(blockPos) + 1.0 >= box.getMinY())
                    return true;
        }
        return false;
    }

    public boolean isTouchingLava(final LocalizedCollisionBox cb) {
        // adjust collision box slightly to avoid false positives at borders?
        final LocalizedCollisionBox box = cb.stretch(-0.001, -0.001, -0.001);
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
            if (blockAtBlockPos == BlockRegistry.LAVA)
                if (BlockPos.getY(blockPos) + 1.0 >= box.getMinY())
                    return true;
        }
        return false;
    }

    public boolean isWater(Block block) {
        return block == BlockRegistry.WATER
            || block == BlockRegistry.BUBBLE_COLUMN;
    }

    public boolean isFluid(Block block) {
        return isWater(block) || block == BlockRegistry.LAVA;
    }

    public List<BlockPos> getBlockPosListInCollisionBox(final LocalizedCollisionBox cb) {
        int minX = MathHelper.floorI(cb.getMinX());
        int maxX = MathHelper.ceilI(cb.getMaxX());
        int minY = MathHelper.floorI(cb.getMinY());
        int maxY = MathHelper.ceilI(cb.getMaxY());
        int minZ = MathHelper.floorI(cb.getMinZ());
        int maxZ = MathHelper.ceilI(cb.getMaxZ());
        final List<BlockPos> blockPosList = new ArrayList<>((maxX - minX) * (maxY - minY) * (maxZ - minZ));
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPosList.add(new BlockPos(x, y, z));
                }
            }
        }
        return blockPosList;
    }

    public LongList getBlockPosLongListInCollisionBox(final LocalizedCollisionBox cb) {
        int minX = MathHelper.floorI(cb.getMinX());
        int maxX = MathHelper.ceilI(cb.getMaxX());
        int minY = MathHelper.floorI(cb.getMinY());
        int maxY = MathHelper.ceilI(cb.getMaxY());
        int minZ = MathHelper.floorI(cb.getMinZ());
        int maxZ = MathHelper.ceilI(cb.getMaxZ());
        final LongArrayList blockPosList = new LongArrayList((maxX - minX) * (maxY - minY) * (maxZ - minZ));
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPosList.add(BlockPos.asLong(x, y, z));
                }
            }
        }
        return blockPosList;
    }

    public List<BlockState> getCollidingBlockStates(final LocalizedCollisionBox cb) {
        final List<BlockState> blockStates = new ArrayList<>(0);
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var blockState = getBlockState(blockPos);
            if (blockState.id() == 0) continue; // air
            blockStates.add(blockState);
        }
        return blockStates;
    }

    public List<LocalizedBlockState> getCollidingLocalizedBlockStates(final LocalizedCollisionBox cb) {
        final List<LocalizedBlockState> blockStates = new ArrayList<>(0);
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var blockState = getBlockState(blockPos);
            if (blockState.id() == 0) continue; // air
            blockStates.add(new LocalizedBlockState(blockState, BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos)));
        }
        return blockStates;
    }

    public static boolean isSpaceEmpty(final LocalizedCollisionBox cb) {
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var x = BlockPos.getX(blockPos);
            var y = BlockPos.getY(blockPos);
            var z = BlockPos.getZ(blockPos);
            var blockStateCBs = getBlockState(blockPos).getLocalizedCollisionBoxes(x, y, z);
            for (int j = 0; j < blockStateCBs.size(); j++) {
                if (blockStateCBs.get(j).intersects(cb)) return false;
            }
        }
        return true;
    }

    public static Optional<BlockPos> findSupportingBlockPos(final LocalizedCollisionBox cb) {
        BlockPos supportingBlock = null;
        double dist = Double.MAX_VALUE;
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos2 = blockPosList.getLong(i);
            var x = BlockPos.getX(blockPos2);
            var y = BlockPos.getY(blockPos2);
            var z = BlockPos.getZ(blockPos2);
            var blockStateCBs = getBlockState(blockPos2).getLocalizedCollisionBoxes(x, y, z);
            for (int j = 0; j < blockStateCBs.size(); j++) {
                if (blockStateCBs.get(j).intersects(cb)) {
                    final double curDist = MathHelper.distanceSq3d(x, y, z, cb.getX(), cb.getY(), cb.getZ());
                    if (curDist < dist || curDist == dist && (supportingBlock == null || BlockPos.compare(supportingBlock.getX(), supportingBlock.getY(), supportingBlock.getZ(), x, y, z) < 0)) {
                        supportingBlock = new BlockPos(x, y, z);
                        dist = curDist;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(supportingBlock);
    }

    public static float getFluidHeight(LocalizedBlockState localBlockState) {
        var block = localBlockState.blockState().block();
        // todo: support waterlogged blocks
        if (block != BlockRegistry.WATER && block != BlockRegistry.LAVA) return 0f;
        if (block == BlockRegistry.WATER) {
            if (World.getBlockAtBlockPos(localBlockState.x(), localBlockState.y() + 1, localBlockState.z()) == BlockRegistry.WATER) {
                return 1;
            }
            int level = localBlockState.blockState().id() - localBlockState.blockState().block().minStateId();
            if ((level & 0x8) == 8) return 8 / 9f;
            return (8 - level) / 9f;
        } else if (block == BlockRegistry.LAVA) {
            if (World.getBlockAtBlockPos(localBlockState.x(), localBlockState.y() + 1, localBlockState.z()) == BlockRegistry.LAVA) {
                return 1;
            }
            int level = localBlockState.blockState().id() - localBlockState.blockState().block().minStateId();
            if (level >= 8) return 8 / 9f;
            return (8 - level) / 9f;
        }
        return 8 / 9f;
    }

    public static MutableVec3d getFluidFlow(LocalizedBlockState localBlockState) {
        float fluidHeight = Math.min(getFluidHeight(localBlockState), 8 / 9f);
        if (fluidHeight == 0) return new MutableVec3d(0, 0, 0);
        double d0 = 0;
        double d1 = 0;
        var directions = asList(new Direction(0, -1), new Direction(1, 0), new Direction(0, 1), new Direction(-1, 0));
        for (var dir : directions) {
            int x = localBlockState.x() + dir.x;
            int y = localBlockState.y();
            int z = localBlockState.z() + dir.z;
            if (affectsFlow(localBlockState, x, y, z)) {
                float f1 = 0.0F;
                var offsetState = new LocalizedBlockState(getBlockState(x, y, z), x, y , z);
                float offsetFluidHeight = Math.min(getFluidHeight(offsetState), 8 / 9f);
                if (offsetFluidHeight == 0) {
                    if (affectsFlow(localBlockState, x, y - 1, z)) {
                        offsetFluidHeight = Math.min(getFluidHeight(new LocalizedBlockState(getBlockState(x, y - 1, z), x, y - 1, z)), 8 / 9f);
                        if (offsetFluidHeight > 0) {
                            f1 = fluidHeight - (offsetFluidHeight - 0.8888889F);
                        }
                    }
                } else if (offsetFluidHeight > 0) {
                    f1 = fluidHeight - offsetFluidHeight;
                }

                if (f1 != 0) {
                    d0 += (float) dir.x * f1;
                    d1 += (float) dir.z * f1;
                }
            }
        }
        var vec3d = new MutableVec3d(d0, 0, d1);

        if (isFluid(localBlockState.blockState().block()) && (localBlockState.blockState().id() - localBlockState.blockState().block().minStateId() >= 8)) {
            for (var dir : directions) {
                var bs = getBlockState(localBlockState.x() + dir.x, localBlockState.y(), localBlockState.z() + dir.z);
                var bsAbove = getBlockState(localBlockState.x() + dir.x, localBlockState.y() + 1, localBlockState.z() + dir.z);
                if (bs.isSolidBlock() || bsAbove.isSolidBlock()) {
                    vec3d.normalize();
                    vec3d.add(0, -6, 0);
                    break;
                }
            }
        }
        vec3d.normalize();
        return vec3d;
    }

    private static boolean affectsFlow(LocalizedBlockState inType, int x, int y, int z) {
        var blockState = getBlockState(x, y, z);
        return !isFluid(blockState.block()) || blockState.block() == inType.blockState().block();
    }


    record Direction(int x, int z) {}
}
