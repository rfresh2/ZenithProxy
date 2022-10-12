package com.zenith.pathing;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import jdk.nashorn.internal.ir.Block;

import static com.zenith.util.Constants.CACHE;

public class World {

    public Chunk getChunk(final ChunkPos chunkPos) {
        return CACHE.getChunkCache().get(chunkPos.getX(), chunkPos.getZ()).getChunks()[chunkPos.getY()];
    }


    public int getBlockId(final int x, final int y, final int z) {
        return getChunk(new ChunkPos(x, y, z)).getBlocks().get(x, y, z).getId();
    }

    public boolean isSolidBlock(final int x, final int y, final int z) {
        return getBlock(getBlockId(x, y, z)).isSolid;
    }
}
