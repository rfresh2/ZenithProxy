package com.zenith.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.zenith.pathing.blockdata.Block;
import com.zenith.pathing.blockdata.BlockData;
import com.zenith.pathing.blockdata.BlockDataManager;
import net.daporkchop.lib.math.vector.Vec3i;

import javax.annotation.Nullable;
import java.util.List;

import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

public class World {
    private static final BlockState AIR = new BlockState(0, 0);

    private final BlockDataManager blockDataManager;
    static final Vec3i downVec = Vec3i.of(0, -150, 0);

    public World(final BlockDataManager blockDataManager) {
        this.blockDataManager = blockDataManager;
    }

    @Nullable
    public Chunk getChunk(final int x, final int y, final int z) {
        try {
            return CACHE.getChunkCache().get(x, z).getChunks()[y];
        } catch (final Exception e) {
//            CLIENT_LOG.error("error finding chunk at pos: {}", chunkPos);
        }
        return null;
    }

    public int getBlockId(final BlockPos blockPos) {
        final Chunk chunk = getChunk(blockPos.getChunkX(), blockPos.getChunkY(), blockPos.getChunkZ());
        if (chunk == null) return 0;
        return chunk.getBlocks().get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15).getId();
    }

    public BlockState getBlockState(final BlockPos blockPos) {
        final Chunk chunk = getChunk(blockPos.getChunkX(), blockPos.getChunkY(), blockPos.getChunkZ());
        if (chunk == null) return AIR;
        return chunk.getBlocks().get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
    }

    public boolean isSolidBlock(final BlockPos blockPos) {
        return blockDataManager.getBlockFromId(getBlockId(blockPos))
                .map(Block::getBoundingBox)
                .map(boundingBox -> boundingBox == BlockData.BoundingBox.BLOCK)
                .orElse(false);
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return blockDataManager.getBlockFromId(getBlockId(blockPos)).orElse(Block.AIR);
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
        final BlockState blockStateAtBlockPos = getBlockState(blockPos);
        final List<CollisionBox> collisionBoxesForStateId = blockAtBlockPos.getCollisionBoxesForStateId(blockStateAtBlockPos.getData());
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
