package com.zenith.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.List;
import java.util.Optional;

import static com.zenith.util.Constants.CACHE;
import static java.util.Arrays.asList;

public class World {

    private final BlockDataManager blockDataManager;
    static final Vec3i downVec = Vec3i.of(0, -1, 0);

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

    public boolean isSolidBlock(final BlockPos blockPos) {
        return blockDataManager.getBlockFromId(getBlockId(blockPos))
                .map(Block::getBoundingBox)
                .map(boundingBox -> boundingBox == BoundingBox.block)
                .orElse(false);
    }

    public Optional<BlockPos> rayTraceCBDown(final Position startPos) {
        return rayTraceCB(startPos, downVec);
    }

    // todo: raytrace in arbitrary direction
    // todo: make this more efficient
    public Optional<BlockPos> rayTraceCB(final Position startPos, final Vec3i ray) {
        final int maxDist = 256; // todo: make this based on max possible distance for loaded chunks or something
        Position pos = startPos;
        if (ray.x() != 0) {
            for (int j = 0; j < maxDist; j++) {
                Optional<BlockPos> intersection = getBlockIntersection(pos);
                if (intersection.isPresent()) return intersection;
                pos = pos.addX(j * Integer.signum(ray.x()));
            }
        } else if (ray.y() != 0) {
            for (int j = 0; j < maxDist; j++) {
                Optional<BlockPos> intersection = getBlockIntersection(pos);
                if (intersection.isPresent()) return intersection;
                pos = startPos.addY(j * Integer.signum(ray.y()));
            }
        } else if (ray.z() != 0) {
            for (int j = 0; j < maxDist; j++) {
                Optional<BlockPos> intersection = getBlockIntersection(pos);
                if (intersection.isPresent()) return intersection;
                pos = startPos.addZ(j * Integer.signum(ray.z()));
            }
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
                if (CollisionBox.playerIntersectsWithBlock(pos, blockPos)) {
                    return Optional.of(blockPos);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> raytraceDown(final BlockPos startPos) {
        return rayTrace(startPos, downVec);
    }

    // raytrace with actual ray, no collision box checks assumes everything has block dimensions
    // only supporting one direction at a time raytrace atm
    // warning: careful using this for player movement checks, use CB raytrace to use collision boxes
    // todo: refactor out common code
    public Optional<BlockPos> rayTrace(final BlockPos startPos, final Vec3i ray) {
        final int maxDist = 256; // todo: make this based on max possible distance for loaded chunks or something
        BlockPos blockPos = startPos;
        if (ray.x() != 0) {
            for (int j = 0; j < maxDist; j++) {
                if (isSolidBlock(blockPos)) {
                    return Optional.of(blockPos);
                }
                blockPos = blockPos.addX(j * Integer.signum(ray.x()));
            }
        } else if (ray.y() != 0) {
            for (int j = 0; j < maxDist; j++) {
                if (isSolidBlock(blockPos)) {
                    return Optional.of(blockPos);
                }
                blockPos = startPos.addY(j * Integer.signum(ray.y()));
            }
        } else if (ray.z() != 0) {
            for (int j = 0; j < maxDist; j++) {
                if (isSolidBlock(blockPos)) {
                    return Optional.of(blockPos);
                }
                blockPos = startPos.addZ(j * Integer.signum(ray.z()));
            }
        }
        return Optional.empty();
    }
}
