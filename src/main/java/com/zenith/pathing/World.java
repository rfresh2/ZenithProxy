package com.zenith.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;

import java.util.Optional;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class World {

    private final BlockDataManager blockDataManager;

    public World(final BlockDataManager blockDataManager) {
        this.blockDataManager = blockDataManager;
    }

    public Optional<Chunk> getChunk(final ChunkPos chunkPos) {
        try {
            return Optional.of(CACHE.getChunkCache().get(chunkPos.getX(), chunkPos.getZ()).getChunks()[chunkPos.getY()]);
        } catch (final Exception e) {
            CLIENT_LOG.error("error finding chunk at pos: {}", chunkPos);
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
}
