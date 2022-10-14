package com.zenith.pathing;

import lombok.Data;

@Data
public class BlockPos {
    private final int x;
    private final int y;
    private final int z;

    public ChunkPos toChunkPos() {
        return new ChunkPos(x >> 4, y >> 4, z >> 4);
    }

    public BlockPos addY(int delta) {
        return new BlockPos(x, y + delta, z);
    }
}
