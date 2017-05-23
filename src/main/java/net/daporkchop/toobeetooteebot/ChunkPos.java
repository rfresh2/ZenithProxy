package net.daporkchop.toobeetooteebot;

/**
 * Created by DaPorkchop_ on 4/18/2017.
 */
public class ChunkPos {

    public int x;
    public int z;
    public long hash;

    public ChunkPos(long hash) {
        this.hash = hash;
        this.x = getXFromHash(hash);
        this.z = getZFromHash(hash);
    }

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
        this.hash = getChunkHashFromXZ(x, z);
    }

    public static long getChunkHashFromXZ(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public static ChunkPos getPosFromHash(long hash) {
        return new ChunkPos(getXFromHash(hash), getZFromHash(hash));
    }

    public static int getXFromHash(long hash) {
        return (int) (hash >> 32);
    }

    public static int getZFromHash(long hash) {
        return (int) hash;
    }
}