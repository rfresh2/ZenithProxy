package com.zenith.pathing;

import lombok.Data;

@Data
public class BlockPos {
    private final int x;
    private final int y;
    private final int z;

//    public ChunkPos toChunkPos() {
//        return new ChunkPos(x >> 4, y >> 4, z >> 4);
//    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkY() {
        return y >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    public BlockPos addX(int delta) {
        return new BlockPos(x + delta, y, z);
    }

    public BlockPos add(final int x, final int y, final int z) {
        return new BlockPos(getX() + x, getY() + y, getZ() + z);
    }

    public BlockPos addY(int delta) {
        return new BlockPos(x, y + delta, z);
    }

    public BlockPos addZ(int delta) {
        return new BlockPos(x, y, z + delta);
    }

    public BlockPos minus(BlockPos other) {
        return new BlockPos(x - other.getX(), y - other.getY(), z - other.getZ());
    }

    public double distance(final BlockPos other) {
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2) + Math.pow(other.z - z, 2));
    }
}
