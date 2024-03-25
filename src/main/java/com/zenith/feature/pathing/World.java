package com.zenith.feature.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.feature.pathing.blockdata.BlockState;
import com.zenith.util.math.MathHelper;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zenith.Shared.*;

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

    public BlockState getBlockState(final int x, final int y, final int z) {
        return new BlockState(getBlockAtBlockPos(new BlockPos(x, y, z)), getBlockStateId(x, y, z), new BlockPos(x, y, z));
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return getBlockAtBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public Block getBlockAtBlockPos(final int x, final int y, final int z) {
        Block blockData = BLOCK_DATA.getBlockDataFromBlockStateId(getBlockStateId(x, y, z));
        if (blockData == null)
            return Block.AIR;
        return blockData;
    }

    public List<LocalizedCollisionBox> getSolidBlockCollisionBoxes(final LocalizedCollisionBox cb) {
        final List<LocalizedCollisionBox> boundingBoxList = new ArrayList<>();
        for (BlockPos blockPos : getBlockPosListInCollisionBox(cb)) {
            final BlockState blockState = getBlockState(blockPos);
            if (blockState.isSolidBlock()) {
                for (CollisionBox collisionBox : blockState.getCollisionBoxes()) {
                    boundingBoxList.add(new LocalizedCollisionBox(collisionBox, blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                }
            }
        }
        return boundingBoxList;
    }

    public boolean isTouchingWater(final LocalizedCollisionBox cb) {
        // adjust collision box slightly to avoid false positives at borders?
        final LocalizedCollisionBox box = cb.stretch(-0.001, -0.001, -0.001);
        for (BlockPos blockPos : getBlockPosListInCollisionBox(box)) {
            final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
            if (isWater(blockAtBlockPos))
                if (blockPos.getY() + 1.0 >= box.getMinY())
                    return true;
        }
        return false;
    }

    private static final int waterId = BLOCK_DATA.getBlockFromName("water").id();
    private static final int bubbleColumnId = BLOCK_DATA.getBlockFromName("bubble_column").id();

    public boolean isWater(Block block) {
        return block.id() == waterId
            || block.id() == bubbleColumnId;
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

    public List<BlockState> getCollidingBlockStates(final LocalizedCollisionBox cb) {
        final List<BlockState> blockStates = new ArrayList<>(0);
        for (BlockPos blockPos : getBlockPosListInCollisionBox(cb)) {
            var blockState = getBlockState(blockPos);
            if (blockState.id() == 0) continue; // air
            blockStates.add(blockState);
        }
        return blockStates;
    }

    public static boolean isSpaceEmpty(final LocalizedCollisionBox cb) {
        for (BlockPos blockPos : getBlockPosListInCollisionBox(cb)) {
            var blockStateCBs = getBlockState(blockPos).getCollisionBoxes();
            for (int i = 0; i < blockStateCBs.size(); i++) {
                var localizedCB = new LocalizedCollisionBox(blockStateCBs.get(i), blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (localizedCB.intersects(cb)) return false;
            }
        }
        return true;
    }

    public static Optional<BlockPos> findSupportingBlockPos(final LocalizedCollisionBox cb) {
        BlockPos supportingBlock = null;
        double dist = Double.MAX_VALUE;
        for (BlockPos blockPos2 : getBlockPosListInCollisionBox(cb)) {
            var blockStateCBs = getBlockState(blockPos2).getCollisionBoxes();
            for (int i = 0; i < blockStateCBs.size(); i++) {
                var localizedCB = new LocalizedCollisionBox(blockStateCBs.get(i), blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
                if (localizedCB.intersects(cb)) {
                    final double curDist = blockPos2.squaredDistance(cb.getX(), cb.getY(), cb.getZ());
                    if (curDist < dist || curDist == dist && (supportingBlock == null || supportingBlock.compareTo(blockPos2) < 0)) {
                        supportingBlock = blockPos2;
                        dist = curDist;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(supportingBlock);
    }
}
