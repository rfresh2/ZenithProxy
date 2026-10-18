package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.feature.pathfinder.process.elytra.pathfinder.NetherPathfinder;

/**
 * @author Brady
 */
public final class BlockStateOctreeInterface {

    private final NetherPathfinder pathfinder;
    private final int minY;

    public BlockStateOctreeInterface(final NetherPathfinderContext context) {
        this.pathfinder = context.context;
        this.minY = context.minY;
    }

    public boolean get0(final int x, final int y, final int z) {
        final int adjustedY = y - this.minY;
        if (adjustedY < 0 || adjustedY > 383) {
            return false;
        }
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        // The tick and background solver both hold read locks. Keep this lookup local.
        var chunk = this.pathfinder.getChunkOrDefault(chunkX, chunkZ, true);
        return chunk.isSolid(x & 0xF, adjustedY, z & 0xF);
    }
}
