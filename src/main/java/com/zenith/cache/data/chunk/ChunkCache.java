package com.zenith.cache.data.chunk;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkBiomeData;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import com.github.steveice10.opennbt.MNBTIO;
import com.github.steveice10.opennbt.mini.MNBT;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.ImmutableMap;
import com.zenith.Proxy;
import com.zenith.Shared;
import com.zenith.cache.CachedData;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MutableVec3i;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.zenith.Shared.*;
import static com.zenith.util.RefStrings.BRAND_SUPPLIER;

@Getter
@Setter
public class ChunkCache implements CachedData {
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    // todo: consider moving weather to a separate cache object
    private boolean isRaining = false;
    private float rainStrength = 0f;
    private float thunderStrength = 0f;
    private int renderDistance = 25;
    protected final ConcurrentHashMap<Long, Chunk> cache = new ConcurrentHashMap<>();
    protected Map<String, Dimension> dimensionRegistry = new ConcurrentHashMap<>();
    protected Dimension currentDimension = null;
    protected Int2ObjectMap<Biome> biomes = new Int2ObjectOpenHashMap<>();
    protected int biomesEntryBitsSize = -1;
    protected WorldData worldData;
    protected int serverViewDistance = -1;
    protected int serverSimulationDistance = -1;
    protected MinecraftCodecHelper codec;
    protected CompoundTag registryTag;
    protected int centerX;
    protected int centerZ;
    // todo: also cache world border size changes
    //  doesn't particularly matter on 2b2t tho
    protected WorldBorderData worldBorderData = WorldBorderData.DEFAULT;
    protected WorldTimeData worldTimeData;
    protected byte[] serverBrand = BRAND_SUPPLIER.get();

    public ChunkCache() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::reapDeadChunks,
                                                       5L,
                                                       5L,
                                                       TimeUnit.MINUTES);
        codec = MinecraftCodec.CODEC.getHelperFactory().get();
    }

    public void updateRegistryTag(final CompoundTag registryData) {
        this.registryTag = registryData;
        setDimensionRegistry(registryData);
        setBiomes(registryData);
    }

    public void setDimensionRegistry(final CompoundTag registryData) {
        ListTag dimensionList = registryData.<CompoundTag>get("minecraft:dimension_type").<ListTag>get("value");
        for (Tag tag : dimensionList) {
            CompoundTag dimension = (CompoundTag) tag;
            String name = dimension.<StringTag>get("name").getValue();
            int id = dimension.<IntTag>get("id").getValue();
            CompoundTag element = dimension.<CompoundTag>get("element");
            int height = element.<IntTag>get("height").getValue();
            int minY = element.<IntTag>get("min_y").getValue();
            dimensionRegistry.put(name, new Dimension(name, id, height, minY));
        }
    }

    public void setBiomes(final CompoundTag registryData) {
        final CompoundTag biomeRegistry = registryData.<CompoundTag>get("minecraft:worldgen/biome");
        for (Tag type : biomeRegistry.<ListTag>get("value").getValue()) {
            CompoundTag biomeNBT = (CompoundTag) type;
            String biomeName = biomeNBT.<StringTag>get("name").getValue();
            int biomeId = biomeNBT.<IntTag>get("id").getValue();
            Biome biome = new Biome(biomeName, biomeId);
            biomes.put(biome.id(), biome);
        }
        biomesEntryBitsSize = log2RoundUp(biomes.size());
    }

    public void setCurrentWorld(final String dimensionType, final String worldName, long hashedSeed, boolean debug, boolean flat) {
        worldData = new WorldData(dimensionType, worldName, hashedSeed, debug, flat);
        currentDimension = dimensionRegistry.get(worldName);
        CACHE_LOG.debug("Updated current world to {}", worldName);
    }

    public static int log2RoundUp(int num) {
        return (int) Math.ceil(Math.log(num) / Math.log(2));
    }

    public static void sync() {
        final ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer == null) return;
        CACHE.getChunkCache().cache.values().parallelStream()
            .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                chunk.x,
                chunk.z,
                chunk.serialize(CACHE.getChunkCache().getCodec()),
                chunk.heightMaps,
                chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                chunk.lightUpdateData))
            .forEach(currentPlayer::sendAsync);
    }

    public boolean isChunkLoaded(final int x, final int z) {
        return cache.containsKey(chunkPosToLong(x, z));
    }

    private static long chunkPosToLong(final int x, final int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public boolean updateBlock(final @NonNull BlockChangeEntry record) {
        // todo: recalculate chunk heightmaps NBT on each block update?
        MutableVec3i pos = MutableVec3i.from(record.getPosition());
        if (pos.getY() < currentDimension.minY || pos.getY() >= currentDimension.minY + currentDimension.height) {
            CLIENT_LOG.warn("Received block update packet for block outside of dimension bounds: pos: {}, minY: {}, height: {}", pos, currentDimension.minY, currentDimension.height);
            return false;
        }

        Chunk chunk = get(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) {
            ChunkSection chunkSection = chunk.sections[(pos.getY() >> 4) - getMinSection()];
            if (chunkSection == null) {
                chunkSection = new ChunkSection(0,
                                                DataPalette.createForChunk(BLOCK_DATA_MANAGER.getBlockBitsPerEntry()),
                                                DataPalette.createForBiome(biomesEntryBitsSize));
            }
            chunkSection.setBlock(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, record.getBlock());
            handleBlockUpdateTileEntity(record, pos, chunkSection, chunk);
        } else {
            CLIENT_LOG.warn("Received block update packet for unknown chunk: {} {}", pos.getX() >> 4, pos.getZ() >> 4);
            return false;
        }
        return true;
    }

    // update any tile entities implicitly affected by this block update
    // server doesn't always send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateTileEntity(BlockChangeEntry record, MutableVec3i pos, ChunkSection section, Chunk chunk) {
        if (record.getBlock() == 0) {
            synchronized (chunk.blockEntities) {
                chunk.blockEntities.removeIf(tileEntity -> tileEntity.getX() == pos.getX() &&
                    tileEntity.getY() == pos.getY() &&
                    tileEntity.getZ() == pos.getZ());
            }
        } else {
            final Block block = BLOCK_DATA_MANAGER.getBlockDataFromBlockStateId(record.getBlock());
            if (block == null) {
                CLIENT_LOG.warn("Received block update packet for unknown block: {}", record.getBlock());
                return;
            }
            final String blockName = block.name();
            if (blockName.equals("chest")) {
                writeTileEntity(chunk, blockName, BlockEntityType.CHEST, pos);
            } else if (blockName.equals("trapped_chest")) {
                writeTileEntity(chunk, blockName, BlockEntityType.TRAPPED_CHEST, pos);
            } else if (blockName.equals("ender_chest")) {
                writeTileEntity(chunk, blockName, BlockEntityType.ENDER_CHEST, pos);
            } else if (blockName.equals("enchanting_table")) {
                writeTileEntity(chunk, blockName, BlockEntityType.ENCHANTING_TABLE, pos);
            } else if (blockName.endsWith("_bed")) {
                writeTileEntity(chunk, blockName, BlockEntityType.BED, pos);
            } else if (blockName.endsWith("_banner")) {
                writeTileEntity(chunk, blockName, BlockEntityType.BANNER, pos);
            }
        }
    }

    private void writeTileEntity(final Chunk chunk, final String blockName, final BlockEntityType type, final MutableVec3i position) {
        // todo: no idea if this compound tag is correct still
        final CompoundTag tileEntityTag = new CompoundTag(blockName, ImmutableMap.of(
            // there's probably more properties some tile entities need but this seems to work well enough
            "id", new StringTag("id", "minecraft:" + blockName),
            "x", new IntTag("x", position.getX()),
            "y", new IntTag("y", position.getY()),
            "z", new IntTag("z", position.getZ())
        ));
        try {
            // todo: improve mem pressure writing MNBT. this method shouldn't be called super frequently and the nbt is small so its ok for now
            final MNBT nbt = MNBTIO.write(tileEntityTag);
            synchronized (chunk.blockEntities) {
                Optional<BlockEntityInfo> foundTileEntity = chunk.blockEntities.stream()
                    .filter(tileEntity -> tileEntity.getX() == position.getX() &&
                        tileEntity.getY() == position.getY() &&
                        tileEntity.getZ() == position.getZ())
                    .findFirst();
                foundTileEntity.ifPresentOrElse(
                    tileEntity -> tileEntity.setNbt(nbt),
                    () -> chunk.blockEntities.add(new BlockEntityInfo(position.getX(),
                                                                      position.getY(),
                                                                      position.getZ(),
                                                                      type,
                                                                      nbt))
                );
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean handleChunkBiomes(final ClientboundChunksBiomesPacket packet) {
        for (ChunkBiomeData biomeData: packet.getChunkBiomeData()) {
            Chunk chunk = this.cache.get(chunkPosToLong(biomeData.getX(), biomeData.getZ()));
            if (chunk == null) {
                CLIENT_LOG.warn("Received chunk biomes packet for unknown chunk: {} {}", biomeData.getX(), biomeData.getZ());
                return false;
            } else {
                ByteBuf buf = Unpooled.wrappedBuffer(biomeData.getBuffer());
                for (int i = 0; i < chunk.sectionsCount; i++) {
                    DataPalette biomesData = codec.readDataPalette(buf, PaletteType.BIOME, biomesEntryBitsSize);
                    chunk.sections[i].setBiomeData(biomesData);
                }
            }
        }
        return true;
    }

    public boolean handleLightUpdate(final ClientboundLightUpdatePacket packet) {
        Chunk chunk = get(packet.getX(), packet.getZ());
        if (chunk != null) {
            chunk.lightUpdateData = packet.getLightData();
        }
        // todo: silently ignoring updates for uncached chunks. should we enqueue them to be processed later?
        return true;
    }

    public boolean multiBlockUpdate(final ClientboundSectionBlocksUpdatePacket packet) {
        for (BlockChangeEntry record : packet.getEntries()) {
            updateBlock(record);
        }
        return true;
    }

    public boolean updateBlock(@NonNull ClientboundBlockUpdatePacket packet) {
        return updateBlock(packet.getEntry());
    }

    public boolean updateTileEntity(final ClientboundBlockEntityDataPacket packet) {
        int chunkX = packet.getPosition().getX() >> 4;
        int chunkZ = packet.getPosition().getZ() >> 4;
        final Chunk chunk = this.cache.get(chunkPosToLong(chunkX, chunkZ));
        if (chunk == null) {
            return false;
        }
        // todo: when we place certain tile entities like beds, the server sends us a block entity update packet with empty nbt
        //  wiki.vg says this should mean the tile entity gets removed
        //  however that doesn't seem to be correct in this case
        //  it's possible there's some interaction with block updates and block change ack packets clients are sent that diverge from this behavior
        synchronized (chunk.blockEntities) {
            final Optional<BlockEntityInfo> existingTileEntity = chunk.blockEntities.stream()
                .filter(tileEntity -> tileEntity.getX() == packet.getPosition().getX() &&
                    tileEntity.getY() == packet.getPosition().getY() &&
                    tileEntity.getZ() == packet.getPosition().getZ())
                .findFirst();
            existingTileEntity.ifPresentOrElse(
                tileEntity -> tileEntity.setNbt(packet.getNbt()),
                () -> chunk.blockEntities.add(new BlockEntityInfo(packet.getPosition().getX(),
                                                                  packet.getPosition().getY(),
                                                                  packet.getPosition().getZ(),
                                                                  packet.getType(),
                                                                  packet.getNbt()))
            );
        }
        return true;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        try {
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
            for (final Chunk chunk : this.cache.values()) {
                consumer.accept(new ClientboundLevelChunkWithLightPacket(
                    chunk.x,
                    chunk.z,
                    chunk.serialize(CACHE.getChunkCache().getCodec()),
                    chunk.heightMaps,
                    chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                    chunk.lightUpdateData));
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
            this.biomes.clear();
            this.biomesEntryBitsSize = -1;
            this.dimensionRegistry.clear();
            this.worldData = null;
            this.currentDimension = null;
            this.serverViewDistance = -1;
            this.serverSimulationDistance = -1;
            this.registryTag = null;
            this.worldBorderData = WorldBorderData.DEFAULT;
            this.worldTimeData = null;
            this.serverBrand = BRAND_SUPPLIER.get();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d chunks", this.cache.size());
    }

    private static int longToChunkX(final long l) {
        return (int) (l & 4294967295L);
    }

    private static int longToChunkZ(final long l) {
        return (int) (l >> 32 & 4294967295L);
    }

    public void add(final ClientboundLevelChunkWithLightPacket p) {
        if (worldData == null) return;
        int chunkX = p.getX();
        int chunkZ = p.getZ();
        byte[] data = p.getChunkData();
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        int sectionsCount = getSectionsCount();
        Chunk existing = cache.get(chunkPosToLong(chunkX, chunkZ));
        Chunk chunk = existing;
        if (existing == null) {
            chunk = new Chunk(chunkX,
                              chunkZ,
                              new ChunkSection[sectionsCount],
                              sectionsCount,
                              Collections.synchronizedList(new ArrayList<>(
                                  List.of(p.getBlockEntities()))),
                              p.getLightData(),
                              p.getHeightMaps());
        }
        for (int i = 0; i < chunk.sectionsCount; i++) {
            chunk.sections[i] = readChunkSection(buf);
        }
        cache.put(chunkPosToLong(chunkX, chunkZ), chunk);
    }

    public ChunkSection readChunkSection(ByteBuf buf) throws UncheckedIOException {
        if (biomesEntryBitsSize == -1) {
            throw new IllegalStateException("Biome entry bits size is not set");
        }

        try {
            int blockCount = buf.readShort();
            DataPalette chunkPalette = codec
                .readDataPalette(buf, PaletteType.CHUNK, Shared.BLOCK_DATA_MANAGER.getBlockBitsPerEntry());
            DataPalette biomePalette = codec
                .readDataPalette(buf, PaletteType.BIOME, biomesEntryBitsSize);
            return new ChunkSection(blockCount, chunkPalette, biomePalette);
        } catch (final IndexOutOfBoundsException e) {
            CACHE_LOG.debug("Error reading chunk section, no data", e);
            return new ChunkSection(0,
                                    DataPalette.createForChunk(BLOCK_DATA_MANAGER.getBlockBitsPerEntry()),
                                    DataPalette.createForBiome(biomesEntryBitsSize));
        }
    }

    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    public int getMaxSection() {
        return ((this.getMaxBuildHeight() - 1) >> 4) + 1;
    }

    public int getMinSection() {
        return currentDimension.minY >> 4;
    }

    public int getMaxBuildHeight() {
        return currentDimension.minY + currentDimension.height;
    }

    public Chunk get(int x, int z) {
        return this.cache.get(chunkPosToLong(x, z));
    }

    // section for blockpos
    public ChunkSection getChunkSection(int x, int y, int z) {
        Chunk chunk = get(x >> 4, z >> 4);
        if (chunk == null) return null;
        int sectionIndex = (y >> 4) - getMinSection();
        if (sectionIndex < 0 || sectionIndex >= chunk.sections.length) {
            return null;
        }
        return chunk.sections[sectionIndex];
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
        final List<Long> toRemove = cache.keySet().stream()
            .filter(key -> distanceOutOfRange(playerX, playerZ, longToChunkX(key), longToChunkZ(key)))
            .toList();
        for (final long l : toRemove) {
            cache.remove(l);
        }
        if (!toRemove.isEmpty()) {
            CLIENT_LOG.debug("Reaped {} dead chunks", toRemove.size());
        }
    }

    private boolean distanceOutOfRange(final int x1, final int y1, final int x2, final int y2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) > maxDistanceExpected;
    }

    public void updateCurrentDimension(final ClientboundRespawnPacket packet) {
        this.currentDimension = dimensionRegistry.get(packet.getDimension());
        this.worldData = new WorldData(packet.getDimension(), // todo: verify if this is even relevant
                                       currentDimension.dimensionName,
                                       packet.getHashedSeed(),
                                       packet.isDebug(),
                                       packet.isFlat());
        CACHE_LOG.debug("Updated current dimension to {}", currentDimension.dimensionName);
    }

    public void updateWorldTime(final ClientboundSetTimePacket packet) {
        if (this.worldTimeData == null) this.worldTimeData = new WorldTimeData();
        else this.worldTimeData.update(packet);
    }
}
