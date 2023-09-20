package com.zenith.feature.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.feature.pathing.blockdata.BlockState;
import com.zenith.util.math.MathHelper;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
        final ChunkSection chunk = getChunkSection(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        if (chunk == null) return 0;
        return chunk.getBlock(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
    }

    public BlockState getBlockState(final BlockPos blockPos) {
        return new BlockState(getBlockAtBlockPos(blockPos), getBlockStateId(blockPos), blockPos);
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return BLOCK_DATA_MANAGER.getBlockFromBlockStateId(getBlockStateId(blockPos)).orElse(Block.AIR);
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
            if (blockAtBlockPos.isWater()) {
                if (blockPos.getY() + 1.0 >= box.getMinY()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<BlockPos> getBlockPosListInCollisionBox(final LocalizedCollisionBox cb) {
        final List<BlockPos> blockPosList = new ArrayList<>();
        int minX = MathHelper.floorToInt(cb.getMinX());
        int maxX = MathHelper.ceilToInt(cb.getMaxX());
        int minY = MathHelper.floorToInt(cb.getMinY());
        int maxY = MathHelper.ceilToInt(cb.getMaxY());
        int minZ = MathHelper.floorToInt(cb.getMinZ());
        int maxZ = MathHelper.ceilToInt(cb.getMaxZ());
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPosList.add(new BlockPos(x, y, z));
                }
            }
        }
        return blockPosList;
    }
}
