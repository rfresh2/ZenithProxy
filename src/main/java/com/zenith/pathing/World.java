package com.zenith.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.zenith.pathing.blockdata.Block;
import com.zenith.pathing.blockdata.BlockData;
import com.zenith.pathing.blockdata.BlockDataManager;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.List;
import java.util.Optional;

import static com.zenith.util.Constants.CACHE;
import static java.util.Arrays.asList;

public class World {
    private static final BlockState AIR = new BlockState(0, 0);

    private final BlockDataManager blockDataManager;
    static final Vec3i downVec = Vec3i.of(0, -150, 0);

    public World(final BlockDataManager blockDataManager) {
        this.blockDataManager = blockDataManager;
    }

    public Optional<Chunk> getChunk(final ChunkPos chunkPos) {
        try {
            return Optional.of(CACHE.getChunkCache().get(chunkPos.getX(), chunkPos.getZ()).getChunks()[chunkPos.getY()]);
        } catch (final Exception e) {
//            CLIENT_LOG.error("error finding chunk at pos: {}", chunkPos);
        }
        return Optional.empty();
    }

    public int getBlockId(final BlockPos blockPos) {
        return getChunk(blockPos.toChunkPos())
                .map(chunk -> chunk.getBlocks().get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15).getId())
                .orElse(0);
    }

    public BlockState getBlockState(final BlockPos blockPos) {
        return getChunk(blockPos.toChunkPos())
                .map(chunk -> chunk.getBlocks().get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15))
                .orElse(AIR);
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

    public Optional<BlockPos> rayTraceCBDown(final Position startPos) {
        return rayTraceCB(startPos, downVec);
    }

    public Optional<BlockPos> rayTraceCB(final Position startPos, final Vec3i ray) {
        Position pos = startPos;
        final Position endPos = startPos.add(ray.x(), ray.y(), ray.z());
        while (!pos.equals(endPos)) {
            final Optional<BlockPos> intersection = getBlockIntersection(pos);
            if (intersection.isPresent()) return intersection;
            pos = pos.add(Integer.signum(ray.x()), Integer.signum(ray.y()), Integer.signum(ray.z()));
        }
        return Optional.empty();
    }

    private Optional<BlockPos> getBlockIntersection(Position pos) {
        final BlockPos center = pos.toBlockPos();
        // this is not very efficient but it works
        final List<BlockPos> surroundingBlockPos = asList(center, center.addX(1), center.addX(-1), center.addZ(1), center.addZ(-1),
                center.addX(1).addZ(1), center.addX(1).addZ(-1),
                center.addX(-1).addZ(1), center.addX(-1).addZ(-1));
        for (BlockPos blockPos : surroundingBlockPos) {
            if (isSolidBlock(blockPos)) {
                if (playerIntersectsWithBlockAtPos(pos, blockPos)) {
                    return Optional.of(blockPos);
                }
            }
        }
        return Optional.empty();
    }

    private boolean playerIntersectsWithBlockAtPos(final Position playerPosition, final BlockPos blockPos) {
        final Block blockAtBlockPos = getBlockAtBlockPos(blockPos);
        final BlockState blockStateAtBlockPos = getBlockState(blockPos);
        final List<CollisionBox> collisionBoxesForStateId = blockAtBlockPos.getCollisionBoxesForStateId(blockStateAtBlockPos.getData());
        return CollisionBox.playerIntersectsWithGenericCollisionBoxes(playerPosition, blockPos, collisionBoxesForStateId);
    }

    public Optional<BlockPos> raytraceDown(final BlockPos startPos) {
        return rayTrace(startPos, downVec);
    }

    // raytrace with blockPos only, no collision box checks
    // warning: careful using this for player movement checks, use CB raytrace to use collision boxes
    public Optional<BlockPos> rayTrace(final BlockPos startPos, final Vec3i ray) {
        BlockPos blockPos = startPos;
        final BlockPos endBlockPos = blockPos.add(ray.x(), ray.y(), ray.z());
        while (!blockPos.equals(endBlockPos)) {
            if (isSolidBlock(blockPos)) {
                return Optional.of(blockPos);
            }
            blockPos = blockPos.add(Integer.signum(ray.x()), Integer.signum(ray.y()), Integer.signum(ray.z()));
        }
        return Optional.empty();
    }
}
