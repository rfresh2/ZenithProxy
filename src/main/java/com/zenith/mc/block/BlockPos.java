package com.zenith.mc.block;

import com.zenith.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public record BlockPos(int x, int y, int z) implements Comparable<BlockPos> {
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
        return new BlockPos(x() + x, y() + y, z() + z);
    }

    public BlockPos addY(int delta) {
        return new BlockPos(x, y + delta, z);
    }

    public BlockPos addZ(int delta) {
        return new BlockPos(x, y, z + delta);
    }

    public BlockPos minus(BlockPos other) {
        return new BlockPos(x - other.x(), y - other.y(), z - other.z());
    }

    public double distance(final BlockPos other) {
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2) + Math.pow(other.z - z, 2));
    }

    public double squaredDistance(final double x, final double y, final double z) {
        double d = (double) this.x() + 0.5 - x;
        double e = (double) this.y() + 0.5 - y;
        double f = (double) this.z() + 0.5 - z;
        return d * d + e * e + f * f;
    }

    private static final int PACKED_X_LENGTH = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000));
    private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    public static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

    public long asLong() {
        return asLong(this.x(), this.y(), this.z());
    }

    public static long asLong(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & PACKED_X_MASK) << X_OFFSET;
        l |= ((long) y & PACKED_Y_MASK);
        return l | ((long) z & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static int getX(long packedPos) {
        return (int) (packedPos << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
    }

    public static int getY(long packedPos) {
        return (int) (packedPos << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int getZ(long packedPos) {
        return (int) (packedPos << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
    }

    public static int compare(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (y1 == y2) {
            return z1 == z2 ? x1 - x2 : z1 - z2;
        } else {
            return y1 - y2;
        }
    }

    public static BlockPos fromLong(long l) {
        return new BlockPos(getX(l), getY(l), getZ(l));
    }

    @Override
    public int compareTo(@NotNull final BlockPos o) {
        if (this.y() == o.y()) {
            return this.z() == o.z() ? this.x() - o.x() : this.z() - o.z();
        } else {
            return this.y() - o.y();
        }
    }
}
