package com.zenith.feature.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.feature.pathing.blockdata.BlockData;
import com.zenith.feature.pathing.blockdata.BlockDataManager;
import net.daporkchop.lib.math.vector.Vec3i;

import javax.annotation.Nullable;
import java.util.List;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;
import static java.util.Arrays.asList;

public class World {
    private static final int AIR = 0;

    private final BlockDataManager blockDataManager;
    static final Vec3i downVec = Vec3i.of(0, -150, 0);

    public World(final BlockDataManager blockDataManager) {
        this.blockDataManager = blockDataManager;
    }

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

    public boolean isSolidBlock(final BlockPos blockPos) {
        return blockDataManager.getBlockFromBlockStateId(getBlockStateId(blockPos))
                .map(Block::getBoundingBox)
                .map(boundingBox -> boundingBox == BlockData.BoundingBox.BLOCK)
                .orElse(false);
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return blockDataManager.getBlockFromBlockStateId(getBlockStateId(blockPos)).orElse(Block.AIR);
    }

    @Nullable
    public BlockPos rayTraceCBDown(final Position startPos) {
        return rayTraceCB(startPos, downVec);
    }

    @Nullable
    public BlockPos rayTraceCB(final Position startPos, final Vec3i ray) {
        Position pos = startPos;
        final Position endPos = startPos.add(ray.x(), ray.y(), ray.z());
        while (!pos.equals(endPos)) {
            final BlockPos intersection = getBlockIntersection(pos);
            if (intersection != null) return intersection;
            pos = pos.add(Integer.signum(ray.x()), Integer.signum(ray.y()), Integer.signum(ray.z()));
        }
        return null;
    }

    @Nullable
    private BlockPos getBlockIntersection(Position pos) {
        final BlockPos center = pos.toBlockPos();
        // this is not very efficient but it works
        final List<BlockPos> surroundingBlockPos = asList(center, center.addX(1), center.addX(-1), center.addZ(1), center.addZ(-1),
                center.addX(1).addZ(1), center.addX(1).addZ(-1),
                center.addX(-1).addZ(1), center.addX(-1).addZ(-1));
        for (BlockPos blockPos : surroundingBlockPos) {
            if (isSolidBlock(blockPos)) {
                if (playerIntersectsWithBlockAtPos(pos, blockPos)) {
                    return blockPos;
                }
            }
        }
        return null;
    }

    private boolean playerIntersectsWithBlockAtPos(final Position playerPosition, final BlockPos blockPos) {
        final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
        final int blockStateAtBlockPos = getBlockStateId(blockPos);
        final List<CollisionBox> collisionBoxesForStateId = blockAtBlockPos.getCollisionBoxesForStateId(blockStateAtBlockPos);
        return CollisionBox.playerIntersectsWithGenericCollisionBoxes(playerPosition, blockPos, collisionBoxesForStateId);
    }

    @Nullable
    public BlockPos raytraceDown(final BlockPos startPos) {
        return rayTrace(startPos, downVec);
    }

    // raytrace with blockPos only, no collision box checks
    // warning: careful using this for player movement checks, use CB raytrace to use collision boxes
    @Nullable
    public BlockPos rayTrace(final BlockPos startPos, final Vec3i ray) {
        BlockPos blockPos = startPos;
        final BlockPos endBlockPos = blockPos.add(ray.x(), ray.y(), ray.z());
        while (!blockPos.equals(endBlockPos)) {
            if (isSolidBlock(blockPos)) {
                return blockPos;
            }
            blockPos = blockPos.add(Integer.signum(ray.x()), Integer.signum(ray.y()), Integer.signum(ray.z()));
        }
        return null;
    }
}
