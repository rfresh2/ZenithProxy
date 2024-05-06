package com.zenith.cache.data.chunk;

import com.github.steveice10.opennbt.mini.MNBT;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.io.MNBTIO;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.feature.world.blockdata.Block;
import com.zenith.feature.world.dimension.DimensionData;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.BrandSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkBiomeData;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.jetbrains.annotations.Nullable;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.zenith.Shared.*;
import static com.zenith.cache.data.chunk.Chunk.*;
import static java.util.Arrays.asList;

@Getter
@Setter
public class ChunkCache implements CachedData {
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    // todo: consider moving weather to a separate cache object
    private boolean isRaining = false;
    private float rainStrength = 0f;
    private float thunderStrength = 0f;
    private int renderDistance = 25; // client-side setting as reported during login, not server. currently unused
    // iterators over this map are not thread safe.
    // to do iteration, copy the key or value set into a new list, then iterate over that copied list.
    // trade-off: faster and lower memory lookups (compared to ConcurrentHashMap), but slower and more memory intensive iteration
    protected final Long2ObjectOpenHashMap<Chunk> cache = new Long2ObjectOpenHashMap<>();
    protected @Nullable DimensionData currentDimension = null;
    protected Int2ObjectOpenHashMap<DimensionData> dimensionRegistry = new Int2ObjectOpenHashMap<>();
    protected List<String> worldNames = new ArrayList<>();
    protected int serverViewDistance = -1;
    protected int serverSimulationDistance = -1;
    protected MinecraftCodecHelper codec;
    protected int centerX;
    protected int centerZ;
    protected int dimensionType;
    protected String worldName;
    protected long hashedSeed;
    protected boolean debug;
    protected boolean flat;
    // todo: also cache world border size changes
    //  doesn't particularly matter on 2b2t tho
    protected WorldBorderData worldBorderData = WorldBorderData.DEFAULT;
    protected WorldTimeData worldTimeData;
    protected byte[] serverBrand = null;

    public ChunkCache() {
        EXECUTOR.scheduleAtFixedRate(this::reapDeadChunks,
                                     5L,
                                     5L,
                                     TimeUnit.MINUTES);
        codec = MinecraftCodec.CODEC.getHelperFactory().get();
        resetDimensionRegistry();
    }

    public void updateDimensionRegistry(final List<RegistryEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        dimensionRegistry.clear();
        for (int id = 0; id < entries.size(); id++) {
            RegistryEntry entry = entries.get(id);
            if (!entry.getId().startsWith("minecraft:")) continue;
            String name = entry.getId().split("minecraft:")[1];
            MNBT tag = entry.getData();
            DimensionData dimensionData;
            if (tag == null) { // occurs when we report to the server we have the core 1.20.6 pack
                // just populate from our own registry
                dimensionData = DIMENSION_DATA.getDimensionData(id);
            } else {
                CompoundTag nbt = (CompoundTag) MNBTIO.read(tag);
                int height = nbt.getNumberTag("height").asInt();
                int minY = nbt.getNumberTag("min_y").asInt();
                dimensionData = new DimensionData(id, name, minY, minY + height, height);
            }
            if (dimensionData == null) {
                CACHE_LOG.error("Undefined dimension registry data for name: {} and ID: {}", name, id);
                dimensionData = DIMENSION_DATA.getDimensionData(0); // fill in with overworld data just so we don't crash ourselves
                continue;
            }
            CACHE_LOG.debug("Adding dimension from registry: {} {} {} {}", name, id, dimensionData.height(), dimensionData.minY());
            dimensionRegistry.put(id, dimensionData);
        }
    }

    public void setCurrentWorld(final int dimensionId, final String worldName, long hashedSeed, boolean debug, boolean flat) {
        this.dimensionType = dimensionId;
        this.worldName = worldName;
        this.hashedSeed = hashedSeed;
        this.debug = debug;
        this.flat = flat;
        var worldDimension = DIMENSION_DATA.getDimensionData(dimensionId);
        if (worldDimension == null) {
            CACHE_LOG.warn("Received unknown dimension ID: {}", dimensionId);
            if (!dimensionRegistry.isEmpty()) {
                worldDimension = DIMENSION_DATA.getDimensionData(0);
                CACHE_LOG.warn("Defaulting to first dimension in registry: {}", worldDimension.name());
            } else {
                throw new RuntimeException("No dimensions in registry");
            }
        }
        this.currentDimension = worldDimension;
        CACHE_LOG.debug("Updated current world to {}", worldName);
        CACHE_LOG.debug("Current dimension: {}", currentDimension.name());
    }

    public static void sync() {
        final ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer == null) return;
        currentPlayer.sendAsync(new ClientboundChunkBatchStartPacket());
        var chunks = new ArrayList<>(CACHE.getChunkCache().cache.values());
        chunks.stream()
            .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                chunk.x,
                chunk.z,
                chunk.sections,
                chunk.heightMaps,
                chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                chunk.lightUpdateData)
            )
            .forEach(currentPlayer::sendAsync);
        currentPlayer.sendAsync(new ClientboundChunkBatchFinishedPacket(chunks.size()));
    }

    public boolean isChunkLoaded(final int x, final int z) {
        return cache.containsKey(chunkPosToLong(x, z));
    }

    public boolean updateBlock(final @NonNull BlockChangeEntry record) {
        try {
            if (record.getY() < currentDimension.minY() || record.getY() >= currentDimension.minY() + currentDimension.height()) {
                // certain client modules might cause the server to send us block updates out of bounds if we send illegal dig packets
                // instead of causing a retry of the block update, just return true and ignore it
                return true;
            }

            final var chunk = get(record.getX() >> 4, record.getZ() >> 4);
            if (chunk != null) {
                var chunkSection = chunk.getChunkSection(record.getY());
                if (chunkSection == null)
                    chunkSection = new ChunkSection(0,
                                                    DataPalette.createForChunk(),
                                                    DataPalette.createForBiome());
                chunkSection.setBlock(record.getX() & 15, record.getY() & 15, record.getZ() & 15, record.getBlock());
                handleBlockUpdateBlockEntity(record, record.getX(), record.getY(), record.getZ(), chunk);
            } else {
                CLIENT_LOG.debug("Received block update packet for unknown chunk: {} {}", record.getX() >> 4, record.getZ() >> 4);
                return false;
            }
        } catch (final Exception e) {
            CLIENT_LOG.debug("Error updating block", e);
            return false;
        }
        return true;
    }

    // update any block entities implicitly affected by this block update
    // server doesn't send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateBlockEntity(BlockChangeEntry record, int x, int y, int z, Chunk chunk) {
        if (record.getBlock() == Block.AIR.id()) {
            synchronized (chunk.blockEntities) {
                chunk.blockEntities.removeIf(tileEntity -> tileEntity.getX() == x &&
                    tileEntity.getY() == y &&
                    tileEntity.getZ() == z);
            }
        } else {
            final var block = BLOCK_DATA.getBlockDataFromBlockStateId(record.getBlock());
            if (block == null) {
                CLIENT_LOG.debug("Received block update packet for unknown block: {}", record.getBlock());
                return;
            }
            final var blockName = block.name();
            final var type = getBlockUpdateBlockEntityTypeOrNull(blockName);
            if (type != null) writeBlockEntity(chunk, blockName, type, x, y, z);
        }
    }

    private BlockEntityType getBlockUpdateBlockEntityTypeOrNull(final String blockName) {
        if (blockName.equals("chest")) {
            return BlockEntityType.CHEST;
        } else if (blockName.equals("trapped_chest")) {
            return BlockEntityType.TRAPPED_CHEST;
        } else if (blockName.equals("ender_chest")) {
            return BlockEntityType.ENDER_CHEST;
        } else if (blockName.equals("enchanting_table")) {
            return BlockEntityType.ENCHANTING_TABLE;
        } else if (blockName.endsWith("_bed")) {
            return BlockEntityType.BED;
        } else if (blockName.endsWith("_banner")) {
            return BlockEntityType.BANNER;
        }
        return null;
    }

    private void writeBlockEntity(final Chunk chunk, final String blockName, final BlockEntityType type, final int x, final int y, final int z) {
        final CompoundTag tileEntityTag = new CompoundTag();
            // there's probably more properties some tile entities need but this seems to work well enough
        tileEntityTag.putString("id", "minecraft:" + blockName);
        tileEntityTag.putInt("x", x);
        tileEntityTag.putInt("y", y);
        tileEntityTag.putInt("z", z);
        // todo: improve mem pressure writing MNBT. this method shouldn't be called super frequently and the nbt is small so its ok for now
        final MNBT nbt = MNBTIO.write(tileEntityTag, false);
        updateOrAddBlockEntity(chunk, x, y, z, type, nbt);
    }

    private void updateOrAddBlockEntity(final Chunk chunk, final int x, final int y, final int z, final BlockEntityType type, final MNBT nbt) {
        synchronized (chunk.blockEntities) {
            boolean found = false;
            for (BlockEntityInfo tileEntity : chunk.blockEntities) {
                if (tileEntity.getX() == x &&
                    tileEntity.getY() == y &&
                    tileEntity.getZ() == z) {
                    tileEntity.setNbt(nbt);
                    found = true;
                    break;
                }
            }
            if (!found) {
                chunk.blockEntities.add(new BlockEntityInfo(x, y, z, type, nbt));
            }
        }
    }

    public boolean handleChunkBiomes(final ClientboundChunksBiomesPacket packet) {
        final var chunkBiomeData = packet.getChunkBiomeData();
        for (int i = 0; i < chunkBiomeData.size(); i++) {
            final ChunkBiomeData biomeData = chunkBiomeData.get(i);
            Chunk chunk = this.cache.get(chunkPosToLong(biomeData.getX(), biomeData.getZ()));
            if (chunk == null) {
                CLIENT_LOG.warn("Received chunk biomes packet for unknown chunk: {} {}",
                                biomeData.getX(),
                                biomeData.getZ());
                return false;
            } else {
                ByteBuf buf = Unpooled.wrappedBuffer(biomeData.getBuffer());
                for (int j = 0; j < chunk.sectionsCount; j++) {
                    DataPalette biomesData = codec.readDataPalette(buf, PaletteType.BIOME);
                    chunk.sections[j].setBiomeData(biomesData);
                }
            }
        }
        return true;
    }

    public boolean handleLightUpdate(final ClientboundLightUpdatePacket packet) {
        final var chunk = get(packet.getX(), packet.getZ());
        if (chunk != null) chunk.lightUpdateData = packet.getLightData();
        // todo: silently ignoring updates for uncached chunks. should we enqueue them to be processed later?
        return true;
    }

    public boolean multiBlockUpdate(final ClientboundSectionBlocksUpdatePacket packet) {
        final var entries = packet.getEntries();
        for (int i = 0; i < entries.length; i++) {
            final BlockChangeEntry record = entries[i];
            updateBlock(record);
        }
        return true;
    }

    public boolean updateBlock(@NonNull ClientboundBlockUpdatePacket packet) {
        return updateBlock(packet.getEntry());
    }

    public boolean updateBlockEntity(final ClientboundBlockEntityDataPacket packet) {
        final int chunkX = packet.getX() >> 4;
        final int chunkZ = packet.getZ() >> 4;
        final var chunk = this.cache.get(chunkPosToLong(chunkX, chunkZ));
        if (chunk == null) return false;
        // when we place certain tile entities like beds, the server sends us a block entity update packet with empty nbt
        //  wiki.vg says this should mean the tile entity gets removed, however that doesn't seem to be correct
        updateOrAddBlockEntity(chunk, packet.getX(), packet.getY(), packet.getZ(), packet.getType(), packet.getNbt());
        return true;
    }

    public @Nullable byte[] getServerBrandRaw() {
        return serverBrand;
    }

    public byte[] getServerBrand() {
        return serverBrand == null
            ? BrandSerializer.defaultBrand(codec)
            : BrandSerializer.appendBrand(codec, serverBrand);
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        try {
            final var brandBytes = getServerBrand();
            consumer.accept(new ClientboundCustomPayloadPacket("minecraft:brand", brandBytes));
            consumer.accept(new ClientboundInitializeBorderPacket(worldBorderData.getCenterX(),
                                                                  worldBorderData.getCenterZ(),
                                                                  worldBorderData.getSize(),
                                                                  worldBorderData.getSize(),
                                                                  0,
                                                                  worldBorderData.getPortalTeleportBoundary(),
                                                                  worldBorderData.getWarningBlocks(),
                                                                  worldBorderData.getWarningTime()));
            consumer.accept(new ClientboundSetChunkCacheRadiusPacket(serverViewDistance));
            consumer.accept(new ClientboundSetChunkCacheCenterPacket(centerX, centerZ));
            if (this.worldTimeData != null) {
                consumer.accept(this.worldTimeData.toPacket());
            }
            consumer.accept(new ClientboundGameEventPacket(GameEvent.LEVEL_CHUNKS_LOAD_START, null));
            consumer.accept(new ClientboundChunkBatchStartPacket());
            var chunks = new ArrayList<>(this.cache.values());
            chunks.stream()
                .sorted(Comparator.comparingInt(chunk -> Math.abs(chunk.x - centerX) + Math.abs(chunk.z - centerZ)))
                .forEach(chunk ->
                    consumer.accept(new ClientboundLevelChunkWithLightPacket(
                        chunk.x,
                        chunk.z,
                        chunk.sections,
                        chunk.heightMaps,
                        chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                        chunk.lightUpdateData))
                );
            consumer.accept(new ClientboundChunkBatchFinishedPacket(this.cache.size()));
            if (CONFIG.debug.sendChunksBeforePlayerSpawn) {
                // todo: this will not handle spectator player cache pos correctly, but we don't have
                //  enough context here to know if this is the spectator or not
                //  at worst, the spectator pos is 1 block off from where we should spawn them anyway
                var player = CACHE.getPlayerCache();
                consumer.accept(new ClientboundPlayerPositionPacket(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), ThreadLocalRandom.current().nextInt(16, 1024)));
            }
        } catch (Exception e) {
            CLIENT_LOG.error("Error getting ChunkData packets from cache", e);
        }
        if (isRaining) {
            consumer.accept(new ClientboundGameEventPacket(GameEvent.START_RAIN, null));
            consumer.accept(new ClientboundGameEventPacket(GameEvent.RAIN_STRENGTH, new RainStrengthValue(this.rainStrength)));
            consumer.accept(new ClientboundGameEventPacket(GameEvent.THUNDER_STRENGTH, new ThunderStrengthValue(this.thunderStrength)));
        }
    }

    @Override
    public void reset(boolean full) {
        this.cache.clear();
        this.isRaining = false;
        this.thunderStrength = 0.0f;
        this.rainStrength = 0.0f;
        if (full) {
            this.dimensionType = 0;
            resetDimensionRegistry();
            this.worldName = null;
            this.hashedSeed = 0;
            this.debug = false;
            this.flat = false;
            this.currentDimension = null;
            this.serverViewDistance = -1;
            this.serverSimulationDistance = -1;
            this.worldBorderData = WorldBorderData.DEFAULT;
            this.worldTimeData = null;
            this.serverBrand = null;
        }
    }

    public synchronized void resetDimensionRegistry() {
        this.dimensionRegistry.clear();
        DIMENSION_DATA.dimensionNames().forEach(name -> {
            var data = DIMENSION_DATA.getDimensionData(name);
            dimensionRegistry.put(data.id(), data);
        });
        worldNames = asList("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d chunks", this.cache.size());
    }

    public void add(final ClientboundLevelChunkWithLightPacket p) {
        final var chunkX = p.getX();
        final var chunkZ = p.getZ();
        ByteBuf buf = Unpooled.wrappedBuffer(p.getChunkData());
        final var sectionsCount = getSectionsCount();
        if (sectionsCount == 0) {
            CACHE_LOG.debug("Received chunk while sectionsCount is 0. Ignoring chunk. Most likely due to a race condition");
            CACHE_LOG.debug("Current dimension: {}", currentDimension);
            return;
        }
        var chunk = cache.get(chunkPosToLong(chunkX, chunkZ));
        if (chunk == null) {
            var blockEntitiesArray = p.getBlockEntities();
            var blockEntities = Collections.synchronizedList(new ArrayList<BlockEntityInfo>(blockEntitiesArray.length));
            Collections.addAll(blockEntities, blockEntitiesArray);
            chunk = new Chunk(
                chunkX,
                chunkZ,
                new ChunkSection[sectionsCount],
                sectionsCount,
                getMaxSection(),
                getMinSection(),
                blockEntities,
                p.getLightData(),
                p.getHeightMaps());
        }
        for (int i = 0; i < chunk.sectionsCount; i++) {
            chunk.sections[i] = readChunkSection(buf);
        }
        cache.put(chunkPosToLong(chunkX, chunkZ), chunk);
    }

    public ChunkSection readChunkSection(ByteBuf buf) throws UncheckedIOException {
        try {
            final var blockCount = buf.readShort();
            final var chunkPalette = codec.readDataPalette(buf, PaletteType.CHUNK);
            final var biomePalette = codec.readDataPalette(buf, PaletteType.BIOME);
            return new ChunkSection(blockCount, chunkPalette, biomePalette);
        } catch (final IndexOutOfBoundsException e) {
            CACHE_LOG.debug("Error reading chunk section, no data", e);
            return new ChunkSection(0, DataPalette.createForChunk(), DataPalette.createForBiome());
        }
    }

    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    public int getMaxSection() {
        return ((this.getMaxBuildHeight() - 1) >> 4) + 1;
    }

    public int getMinSection() {
        var dim = currentDimension;
        return dim != null ? dim.minY() >> 4 : 0;
    }

    public int getMaxBuildHeight() {
        var dim = currentDimension;
        return dim != null ? dim.buildHeight() : 0;
    }

    public Chunk get(int x, int z) {
        return this.cache.get(chunkPosToLong(x, z));
    }

    // section for blockpos
    public ChunkSection getChunkSection(int x, int y, int z) {
        final var chunk = get(x >> 4, z >> 4);
        if (chunk == null) return null;
        return chunk.getChunkSection(y);
    }

    public void remove(int x, int z) {
        this.cache.remove(chunkPosToLong(x, z));
    }

    // reap any chunks we possibly didn't remove from the cache
    // dead chunks could occur due to race conditions, packet ordering, or bad server
    // doesn't need to be invoked frequently and this is not a condition that happens normally
    // i'm adding this because we are very memory constrained
    private void reapDeadChunks() {
        if (!Proxy.getInstance().isConnected()) return;
        final int playerX = ((int) CACHE.getPlayerCache().getX()) >> 4;
        final int playerZ = ((int) CACHE.getPlayerCache().getZ()) >> 4;
        var keys = new LongArrayList(cache.keySet());
        var removedCount = new AtomicInteger(0);
        keys.longStream()
            .filter(key -> distanceOutOfRange(playerX, playerZ, longToChunkX(key), longToChunkZ(key)))
            .forEach(key -> {
                cache.remove(key);
                removedCount.getAndIncrement();
            });
        if (removedCount.get() > 0) {
            CLIENT_LOG.debug("Reaped {} dead chunks", removedCount.get());
        }
    }

    private boolean distanceOutOfRange(final int x1, final int y1, final int x2, final int y2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) > maxDistanceExpected;
    }

    public void updateCurrentDimension(final ClientboundRespawnPacket packet) {
        PlayerSpawnInfo info = packet.getCommonPlayerSpawnInfo();
        CACHE_LOG.debug("Updating current dimension to: {}", info.getDimension());
        DimensionData newDim = dimensionRegistry.get(info.getDimension());
        if (newDim == null) {
            CACHE_LOG.error("Respawn packet tried updating dimension to unregistered dimension: {}", info.getDimension());
            CACHE_LOG.error("Things are going to break...");
        } else {
            this.currentDimension = newDim;
            this.worldName = currentDimension.name();
        }
        this.dimensionType = info.getDimension();
        this.hashedSeed = info.getHashedSeed();
        this.debug = info.isDebug();
        this.flat = info.isFlat();
        CACHE_LOG.debug("Updated current dimension to {}", newDim);
    }

    public void updateWorldTime(final ClientboundSetTimePacket packet) {
        if (this.worldTimeData == null) this.worldTimeData = new WorldTimeData();
        else this.worldTimeData.update(packet);
    }
}
