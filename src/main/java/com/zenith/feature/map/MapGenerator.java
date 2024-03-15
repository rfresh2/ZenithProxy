package com.zenith.feature.map;

import com.github.steveice10.mc.protocol.data.game.chunk.BitStorage;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.io.MNBTIO;
import com.zenith.cache.data.chunk.Chunk;
import com.zenith.feature.pathing.World;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.zenith.Shared.*;
import static com.zenith.cache.data.chunk.Chunk.chunkPosToLong;

public class MapGenerator {

    @SneakyThrows
    public static byte[] generateMapData() {
        return generateMapData(128, false);
    }

    @SneakyThrows
    public static byte[] generateMapData(final int size) {
        return generateMapData(size, false);
    }

    @SneakyThrows
    public static byte[] generateMapData(final int size, final boolean cachedHeightMap) {
        final int chunksSize = size / 16;
        final int dataSize = size * size;
        final int halfWChunks = chunksSize / 2;
        final byte[] data = new byte[dataSize];

        var centerX = CACHE.getChunkCache().getCenterX();
        var centerZ = CACHE.getChunkCache().getCenterZ();

        final int minChunkX = centerX - halfWChunks;
        final int minChunkZ = centerZ - halfWChunks;
        final int maxChunkX = centerX + halfWChunks;
        final int maxChunkZ = centerZ + halfWChunks;

        final int minBlockX = minChunkX * 16;
        final int minBlockZ = minChunkZ * 16;
        final int maxBlockX = maxChunkX * 16;
        final int maxBlockZ = maxChunkZ * 16;
        final Long2ObjectMap<BitStorage> chunkToHeightMap = cachedHeightMap
            ? getCachedHeightMap(minChunkX, minChunkZ, maxChunkX, maxChunkZ)
            : generateHeightMapFromChunkData(minChunkX, minChunkZ, maxChunkX, maxChunkZ);

        for (int x = minBlockX; x < maxBlockX; x++) {
            double d0 = 0.0;
            for (int z = minBlockZ; z < maxBlockZ; z++) {
                final int sectionX = x & 15;
                final int sectionZ = z & 15;
                final int chunkX = x >> 4;
                final int chunkZ = z >> 4;
                final Chunk chunk = CACHE.getChunkCache().get(chunkX, chunkZ);
                if (chunk == null) continue;
                final BitStorage heightsStorage = chunkToHeightMap.get(chunkPosToLong(chunkX, chunkZ));
                if (heightsStorage == null) continue;
                final int relChunkX = chunkX - (centerX - halfWChunks);
                final int relChunkZ = chunkZ - (centerZ - halfWChunks);

                int i0 = 0;
                double d1 = 0.0;

                int height = getHeight(x, z, heightsStorage, (chunk.getMinSection() << 4));
                int blockStateId = chunk.getBlockStateId(sectionX, height, sectionZ);
                Block block = BLOCK_DATA_MANAGER.getBlockDataFromBlockStateId(blockStateId);
                int mapColorId = 0;
                if (block != null) mapColorId = MAP_BLOCK_COLOR_MANAGER.getMapColorId(block.name());
                while (mapColorId == 0 && height > chunk.minY()) {
                    blockStateId = chunk.getBlockStateId(sectionX, --height, sectionZ);
                    block = BLOCK_DATA_MANAGER.getBlockDataFromBlockStateId(blockStateId);
                    if (block != null) mapColorId = MAP_BLOCK_COLOR_MANAGER.getMapColorId(block.name());
                }
                if (height > chunk.minY() && World.isWater(block)) {
                    int yUnderBlock = height - 1;
                    int blockStateId2;
                    Block block2;
                    do {
                        blockStateId2 = chunk.getBlockStateId(sectionX, yUnderBlock--, sectionZ);
                        block2 = BLOCK_DATA_MANAGER.getBlockDataFromBlockStateId(blockStateId2);
                        i0++; // water brightness shading
                    } while (yUnderBlock > chunk.minY() && World.isWater(block2));
                }

                d1 += height;

                Brightness brightness;
                if (mapColorId == 12) { // water
                    final double d2 = (double)i0 * 0.1 + (double)(x + z & 1) * 0.2;
                    if (d2 < 0.5) {
                        brightness = Brightness.HIGH;
                    } else if (d2 > 0.9) {
                        brightness = Brightness.LOW;
                    } else {
                        brightness = Brightness.NORMAL;
                    }
                } else {
                    final double d3 = (d1 - d0) * 4.0 / 5.0 + ((double)(x + z & 1) - 0.5) * 0.4;
                    if (d3 > 0.6) {
                        brightness = Brightness.HIGH;
                    } else if (d3 < -0.6) {
                        brightness = Brightness.LOW;
                    } else {
                        brightness = Brightness.NORMAL;
                    }
                }
                d0 = d1;
                final byte packedId = MAP_BLOCK_COLOR_MANAGER.getPackedId(mapColorId, brightness);
                final int rowX = relChunkX * 16 + sectionX;
                final int rowZ = relChunkZ * 16 + sectionZ;
                data[rowX + rowZ * size] = packedId;
            }
        }

        return data;
    }

    /**
     * The issue with the cached height maps is that we don't update our cached values like the vanilla MC client does
     * so this data will fall out of sync and cause divergence of what the player sees and what's generated
     * This would only occur if there were individual block updates that changed the height map
     *
     * We could update our chunk cache to constantly update the height map data, but that would cause extra GC and cpu pressure
     * and we only use it here, so it's not worth it
     * maybe if there was some need to update the heightmaps for players having issues or something
     */
    @NotNull
    private static Long2ObjectMap<BitStorage> getCachedHeightMap(final int minChunkX, final int minChunkZ, final int maxChunkX, final int maxChunkZ) throws IOException {
        final Long2ObjectMap<BitStorage> chunkToHeightMap = new Long2ObjectOpenHashMap<>((maxChunkX - minChunkX) * (maxChunkZ - minChunkZ));
        for (int chunkX = minChunkX; chunkX < maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ < maxChunkZ; chunkZ++) {
                final Chunk chunk = CACHE.getChunkCache().get(chunkX, chunkZ);
                if (chunk == null) continue;
                final BitStorage heightsStorage = getCachedHeightMapData(chunk);
                chunkToHeightMap.put(chunkPosToLong(chunkX, chunkZ), heightsStorage);
            }
        }
        return chunkToHeightMap;
    }

    private static BitStorage getCachedHeightMapData(Chunk chunk) throws IOException {
        var heightMaps = chunk.getHeightMaps();
        var heightMapNBT = (CompoundTag) MNBTIO.read(heightMaps);
        long[] worldSurfaces = heightMapNBT.getLongArrayTag("WORLD_SURFACE").getValue();
        int bitsPerEntry = MathHelper.log2Ceil((chunk.getMaxSection() << 4) + 1);
        return new BitStorage(bitsPerEntry, 256, worldSurfaces);
    }

    @NotNull
    private static Long2ObjectMap<BitStorage> generateHeightMapFromChunkData(final int minChunkX, final int minChunkZ, final int maxChunkX, final int maxChunkZ) throws IOException {
        final Long2ObjectMap<BitStorage> chunkToHeightMap = new Long2ObjectOpenHashMap<>((maxChunkX - minChunkX) * (maxChunkZ - minChunkZ));
        for (int chunkX = minChunkX; chunkX < maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ < maxChunkZ; chunkZ++) {
                final Chunk chunk = CACHE.getChunkCache().get(chunkX, chunkZ);
                if (chunk == null) continue;
                final BitStorage heightsStorage = generateHeightMapData(chunk);
                chunkToHeightMap.put(chunkPosToLong(chunkX, chunkZ), heightsStorage);
            }
        }
        return chunkToHeightMap;
    }

    private static BitStorage generateHeightMapData(final Chunk chunk) {
        final int minBuildHeight = chunk.minY();
        final int maxBuildHeight = chunk.maxY();
        long[] worldSurfaces = new long[37];
        int bitsPerEntry = MathHelper.log2Ceil((chunk.getMaxSection() << 4) + 1);
        final BitStorage storage = new BitStorage(bitsPerEntry, 256, worldSurfaces);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxBuildHeight; y > minBuildHeight; y--) {
                    final int blockStateId = chunk.getBlockStateId(x, y, z);
                    Block block = BLOCK_DATA_MANAGER.getBlockDataFromBlockStateId(blockStateId);
                    if (block != null && block.id() != Block.AIR.id()) {
                        int index = x + z * 16;
                        storage.set(index, y - minBuildHeight);
                        break;
                    }
                }
            }
        }
        return storage;
    }

    private static int getHeight(int blockX, int blockZ, BitStorage data, int minBuildHeight) {
        return getFirstAvailable(blockX & 15, blockZ & 15, data, minBuildHeight);
    }

    private static int getFirstAvailable(int i, BitStorage data, int minBuildHeight) {
        return data.get(i) + minBuildHeight;
    }

    private static int getFirstAvailable(int i, int j, BitStorage data, int minBuildHeight) {
        return getFirstAvailable(getIndex(i, j), data, minBuildHeight);
    }

    private static int getIndex(int i, int j) {
        return i + j * 16;
    }
}
