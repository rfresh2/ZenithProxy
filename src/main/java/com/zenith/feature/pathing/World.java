package com.zenith.feature.pathing;

import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.feature.pathing.blockdata.BlockState;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.experimental.UtilityClass;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;

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

    public BlockState getBlockState(final long blockPos) {
        return getBlockState(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos));
    }

    public BlockState getBlockState(final int x, final int y, final int z) {
        return new BlockState(getBlockAtBlockPos(new BlockPos(x, y, z)), getBlockStateId(x, y, z), new BlockPos(x, y, z));
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
            return Block.AIR;
        return blockData;
    }

    public List<LocalizedCollisionBox> getSolidBlockCollisionBoxes(final LocalizedCollisionBox cb) {
        final List<LocalizedCollisionBox> boundingBoxList = new ArrayList<>();
        for (long blockPos : getBlockPosLongListInCollisionBox(cb)) {
            var x = BlockPos.getX(blockPos);
            var y = BlockPos.getY(blockPos);
            var z = BlockPos.getZ(blockPos);
            final BlockState blockState = getBlockState(blockPos);
            if (blockState.isSolidBlock()) {
                for (CollisionBox collisionBox : blockState.getCollisionBoxes()) {
                    boundingBoxList.add(new LocalizedCollisionBox(collisionBox, x, y, z));
                }
            }
        }
        return boundingBoxList;
    }

    public boolean isTouchingWater(final LocalizedCollisionBox cb) {
        // adjust collision box slightly to avoid false positives at borders?
        final LocalizedCollisionBox box = cb.stretch(-0.001, -0.001, -0.001);
        for (long blockPos : getBlockPosLongListInCollisionBox(box)) {
            final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
            if (isWater(blockAtBlockPos))
                if (BlockPos.getY(blockPos) + 1.0 >= box.getMinY())
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
        for (long blockPos : getBlockPosLongListInCollisionBox(cb)) {
            var blockState = getBlockState(blockPos);
            if (blockState.id() == 0) continue; // air
            blockStates.add(blockState);
        }
        return blockStates;
    }

    public static boolean isSpaceEmpty(final LocalizedCollisionBox cb) {
        for (long blockPos : getBlockPosLongListInCollisionBox(cb)) {
            var x = BlockPos.getX(blockPos);
            var y = BlockPos.getY(blockPos);
            var z = BlockPos.getZ(blockPos);
            var blockStateCBs = getBlockState(blockPos).getCollisionBoxes();
            for (int i = 0; i < blockStateCBs.size(); i++) {
                var localizedCB = new LocalizedCollisionBox(blockStateCBs.get(i), x, y, z);
                if (localizedCB.intersects(cb)) return false;
            }
        }
        return true;
    }

    public static Optional<BlockPos> findSupportingBlockPos(final LocalizedCollisionBox cb) {
        BlockPos supportingBlock = null;
        double dist = Double.MAX_VALUE;
        for (long blockPos2 : getBlockPosLongListInCollisionBox(cb)) {
            var blockStateCBs = getBlockState(blockPos2).getCollisionBoxes();
            var x = BlockPos.getX(blockPos2);
            var y = BlockPos.getY(blockPos2);
            var z = BlockPos.getZ(blockPos2);
            for (int i = 0; i < blockStateCBs.size(); i++) {
                var localizedCB = new LocalizedCollisionBox(blockStateCBs.get(i), x, y, z);
                if (localizedCB.intersects(cb)) {
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
}
