package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.feature.pathfinder.process.elytra.pathfinder.Chunk;
import com.zenith.feature.pathfinder.process.elytra.pathfinder.NetherPathfinder;
import com.zenith.feature.pathfinder.process.elytra.pathfinder.Raytracer;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.dimension.DimensionRegistry;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Brady
 */
public final class NetherPathfinderContext implements IElytraPathFinder {

    // Packing, block updates, unloads and culling take the write lock. Searches and the
    // flight solver take the read lock so each sees a consistent packed in-memory world.
    public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    public final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();
    private final int maxHeight;

    // Visible for access in BlockStateOctreeInterface
    final NetherPathfinder context;
    // write locked operations
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("Elytra Chunks").factory());
    // operations that don't make changes to the chunk cache. could use multiple threads but i'm not sure if it would cause problems.
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("Elytra Path Search").factory());
    final int minY;
    private final BlockStateOctreeInterface boi;

    public NetherPathfinderContext() {
        var dimension = World.getCurrentDimension();
        this.minY = dimension.minY();
        final NetherPathfinder.Dimension dim;
        if (dimension == DimensionRegistry.THE_NETHER.get()) dim = NetherPathfinder.Dimension.NETHER;
        else if (dimension == DimensionRegistry.THE_END.get()) dim = NetherPathfinder.Dimension.END;
        else dim = NetherPathfinder.Dimension.OVERWORLD;
        int height = Math.min(dimension.height(), 384);
        if (!false /* Baritone.settings().elytraAllowAboveRoof */ && dim == NetherPathfinder.Dimension.NETHER) height = Math.min(height, 128);
        this.maxHeight = height;
        this.context = new NetherPathfinder(dim, height);
        this.boi = new BlockStateOctreeInterface(this);
    }

    public boolean hasChunk(BlockPos pos) {
        return this.context.hasChunkFromCaller(pos.getChunkX(), pos.getChunkZ());
    }

    public void queueCacheCulling(int chunkX, int chunkZ, int maxDistanceBlocks) {
        this.writeExecutor.execute(() -> {
            writeLock.lock();
            try {

                this.context.cullFarChunks(chunkX, chunkZ, maxDistanceBlocks);
            } finally {
                writeLock.unlock();
            }
        });
    }

    public void queueForPacking(final com.zenith.cache.data.chunk.Chunk chunkIn) {
        // Copy palettes on the packet/tick thread; packing must not race a palette resize.
        var sections = new ChunkSection[Math.min(chunkIn.getSections().length, 24)];
        for (int i = 0; i < sections.length; i++) {
            var section = chunkIn.getSections()[i];
            if (section != null && !section.isBlockCountEmpty()) sections[i] = new ChunkSection(section);
        }
        int x = chunkIn.getX();
        int z = chunkIn.getZ();
        this.writeExecutor.execute(() -> {
            writeLock.lock();
            try {
                var packed = this.context.allocateAndInsertChunk(x, z);
                writeChunkData(sections, packed);
            } finally {
                writeLock.unlock();
            }
        });
    }

    public void queueBlockUpdate(int x, int y, int z, int blockStateId) {
        this.writeExecutor.execute(() -> {
            writeLock.lock();
            try {
                var chunk = this.context.getChunk(x >> 4, z >> 4);
                int yy = y - minY;
                if (chunk != null && yy >= 0 && yy < maxHeight) {
                    chunk.setBlock(x & 15, yy, z & 15, isSolid(blockStateId));
                }
            } finally {
                writeLock.unlock();
            }
        });
    }

    public void queueChunkUnload(int x, int z) {
        this.writeExecutor.execute(() -> {
            writeLock.lock();
            try {

                this.context.removeChunk(x, z);
            } finally {
                writeLock.unlock();
            }
        });
    }

    public CompletableFuture<UnpackedSegment> pathFindAsync(final BlockPos src, final BlockPos dst) {
        var adjustedSrc = src.below(minY);
        var adjustedDst = dst.below(minY);
        return CompletableFuture.runAsync(() -> {}, writeExecutor).thenApplyAsync(ignored -> {
            readLock.lock();
            try {
                var segment = this.context.pathFind(
                        adjustedSrc.x(), adjustedSrc.y(), adjustedSrc.z(),
                        adjustedDst.x(), adjustedDst.y(), adjustedDst.z(),
                        !false /* Baritone.settings().elytraAllowTightSpaces */, // atleastX4
                        false, // refine
                        10000, // timeoutMs
                        true, // provisional routing beyond loaded chunks; flight collision checks treat them as solid
                        // TODO: Determine appropriate cost value
                        8.0 // fakeChunkCost
                );
                if (segment == null) {
                    throw new PathCalculationException("Path calculation failed");
                }

                return new UnpackedSegment(UnpackedSegment.from(segment).collect().stream().map(pos -> pos.above(minY)), segment.finished);
            } finally {
                readLock.unlock();
            }
        }, readExecutor);
    }

    /**
     * Performs a raytrace from the given start position to the given end position, returning {@code true} if there is
     * visibility between the two points.
     *
     * @param startX The start X coordinate
     * @param startY The start Y coordinate
     * @param startZ The start Z coordinate
     * @param endX   The end X coordinate
     * @param endY   The end Y coordinate
     * @param endZ   The end Z coordinate
     * @return {@code true} if there is visibility between the points
     */
    public boolean raytrace(final double startX, final double startY, final double startZ,
                            final double endX, final double endY, final double endZ) {
        return Raytracer.raytrace(this.context, startX, startY - this.minY, startZ, endX, endY - this.minY, endZ, NetherPathfinder.CacheMiss.SOLID) == null;
    }

    /**
     * Performs a raytrace from the given start position to the given end position, returning {@code true} if there is
     * visibility between the two points.
     *
     * @param start The starting point
     * @param end   The ending point
     * @return {@code true} if there is visibility between the points
     */
    public boolean raytrace(final Vector3d start, final Vector3d end) {
        return raytrace(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    public boolean raytrace(final int count, final double[] src, final double[] dst, final int visibility) {
        if (src.length != count * 3 || dst.length != count * 3) {
            throw new IllegalArgumentException("Bad array lengths");
        }

        switch (visibility) {
            case Visibility.ALL:
                for (int i = 0; i < count; i++) {
                    if (!clear(src, dst, i)) {
                        return false;
                    }
                }
                return true;
            case Visibility.NONE:
                for (int i = 0; i < count; i++) {
                    if (clear(src, dst, i)) {
                        return false;
                    }
                }
                return true;
            case Visibility.ANY:
                for (int i = 0; i < count; i++) {
                    if (clear(src, dst, i)) {
                        return true;
                    }
                }
                return false;
            default:
                throw new IllegalArgumentException("lol");
        }
    }

    /** Whether ray i of a batch in world coordinates reaches its end. */
    private boolean clear(double[] src, double[] dst, int i) {
        final int o = i * 3;
        return Raytracer.raytrace(this.context, src[o], src[o + 1] - minY, src[o + 2], dst[o], dst[o + 1] - minY, dst[o + 2], NetherPathfinder.CacheMiss.SOLID) == null;
    }

    public void raytrace(final int count, final double[] src, final double[] dst, final boolean[] hitsOut, final double[] hitPosOut) {
        if (src.length != count * 3 || dst.length != count * 3) {
            throw new IllegalArgumentException("Bad array lengths");
        }

        for (int i = 0; i < count; i++) {
            final int o = i * 3;
            var hit = Raytracer.raytrace(this.context, src[o], src[o + 1] - minY, src[o + 2], dst[o], dst[o + 1] - minY, dst[o + 2], NetherPathfinder.CacheMiss.SOLID);
            hitsOut[i] = hit != null;
            if (hit != null && hitPosOut != null) {
                hitPosOut[o] = hit.getX();
                hitPosOut[o + 1] = hit.getY() + this.minY;
                hitPosOut[o + 2] = hit.getZ();
            }
        }
    }

    public boolean passable(int x, int y, int z) {
        return !this.boi.get0(x, y, z);
    }

    public void cancel() {
        this.context.cancel();
    }

    public void destroy() {
        this.cancel();
        // Ignore anything that was queued up, just shutdown the executor
        this.readExecutor.shutdownNow();
        this.writeExecutor.shutdownNow();

        try {
            while (!this.readExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
            while (!this.writeExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        this.context.close();
    }


    public void acquireReadLock() {
        this.readLock.lock();
    }

    public boolean tryAcquireReadLock() {
        return this.readLock.tryLock();
    }

    public void releaseReadLock() {
        this.readLock.unlock();
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    private static boolean isSolid(int stateId) {
        var block = World.getBlock(stateId);
        return !block.isAir() && block != BlockRegistry.RED_MUSHROOM && block != BlockRegistry.BROWN_MUSHROOM;
    }

    private void writeChunkData(ChunkSection[] sections, Chunk packed) {
        for (int y = 0; y < maxHeight && (y >> 4) < sections.length; y++) {
            var section = sections[y >> 4];
            if (section == null) continue;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (isSolid(section.getBlock(x, y & 15, z))) {
                        packed.setBlock(x, y, z, true);
                    }
                }
            }
        }
    }

    public static final class Visibility {
        public static final int ALL = 0;
        public static final int NONE = 1;
        public static final int ANY = 2;
        private Visibility() {}
    }
}
