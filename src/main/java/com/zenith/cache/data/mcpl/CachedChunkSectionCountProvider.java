package com.zenith.cache.data.mcpl;

import com.zenith.feature.world.dimension.DimensionData;
import org.geysermc.mcprotocollib.protocol.ChunkSectionCountProvider;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CACHE_LOG;

/**
 * A local cache run on the packet event loop for ClientboundLevelChunkWithLight packet deserialization.
 *
 * This is purposefully not run and should not be called from the client event loop to avoid data desync
 *
 * Basically all we need to do is track the current dimension, which will give us the correct section count.
 */
public class CachedChunkSectionCountProvider implements ChunkSectionCountProvider {

    protected int sectionCount = 0;

    @Override
    public int getSectionCount() {
        return sectionCount;
    }

    public void updateDimension(final PlayerSpawnInfo spawnInfo) {
        DimensionData newDim = CACHE.getChunkCache().getDimensionRegistry().get(spawnInfo.getDimension());
        if (newDim == null) {
            CACHE_LOG.error("No dimension found for {}", spawnInfo.getDimension());
            CACHE_LOG.error("Things are going to break...");
        } else {
            this.sectionCount = getSectionCount(newDim);
        }
    }

    private int getSectionCount(DimensionData dim) {
        return this.getMaxSection(dim) - this.getMinSection(dim);
    }

    private int getMaxSection(DimensionData dim) {
        return ((this.getMaxBuildHeight(dim) - 1) >> 4) + 1;
    }

    private int getMinSection(DimensionData dim) {
        return dim != null ? dim.minY() >> 4 : 0;
    }

    private int getMaxBuildHeight(DimensionData dim) {
        return dim != null ? dim.buildHeight() : 0;
    }
}
