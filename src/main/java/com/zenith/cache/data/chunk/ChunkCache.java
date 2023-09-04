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
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.ImmutableMap;
import com.zenith.Proxy;
import com.zenith.Shared;
import com.zenith.cache.CachedData;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Vec3i;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

@Getter
@Setter
public class ChunkCache implements CachedData {
    private static final Vec3i DEFAULT_SPAWN_POSITION = new Vec3i(0, 0, 0);
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    protected Vec3i spawnPosition = DEFAULT_SPAWN_POSITION;
    // todo: consider moving weather to a separate cache object
    private boolean isRaining = false;
    private float rainStrength = 0f;
    private float thunderStrength = 0f;
    private int renderDistance = 25;
    protected final Long2ObjectOpenHashMap<Chunk> cache = new Long2ObjectOpenHashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
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

    public ChunkCache() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::reapDeadChunks, 5L, 5L, TimeUnit.MINUTES);
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
            // todo: cache more data from the nbt?
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
    }

    public static int log2RoundUp(int num) {
        return (int) Math.ceil(Math.log(num) / Math.log(2));
    }

    public static void sync() {
        final ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer == null) return;
        CACHE.getChunkCache().readCache(() -> {
            CACHE.getChunkCache().cache.values().parallelStream()
                .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                    chunk.x,
                    chunk.z,
                    chunk.serialize(CACHE.getChunkCache().getCodec()),
                    new CompoundTag("heightmap"), // todo: verify if we need to populate heightmaps
                    chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                    chunk.lightUpdateData))
                .forEach(currentPlayer::send);
            return true;
        });
    }

    private static long chunkPosToLong(final int x, final int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public boolean updateBlock(final @NonNull BlockChangeEntry record) {
        Vec3i pos = Vec3i.from(record.getPosition());
        if (pos.getY() < currentDimension.minY || pos.getY() >= currentDimension.minY + currentDimension.height) {
            CLIENT_LOG.warn("Received block update packet for block outside of dimension bounds: pos: {}, minY: {}, height: {}", pos, currentDimension.minY, currentDimension.height);
            return false;
        }
        Boolean b = writeCache(() -> {
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
            }
            return Boolean.TRUE;
        });
        if (b == null || !b) {
            CLIENT_LOG.warn("Received block update packet for unknown chunk: {} {}", pos.getX() >> 4, pos.getZ() >> 4);
            return false;
        }
        return true;
    }

    // update any tile entities implicitly affected by this block update
    // server doesn't always send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateTileEntity(BlockChangeEntry record, Vec3i pos, ChunkSection section, Chunk chunk) {
        if (record.getBlock() == 0) {
            chunk.blockEntities.removeIf(tileEntity -> tileEntity.getX() == pos.getX() &&
                tileEntity.getY() == pos.getY() &&
                tileEntity.getZ() == pos.getZ());
        } else {
            String blockName = BLOCK_DATA_MANAGER.getBlockFromBlockStateId(record.getBlock()).map(Block::getName).orElse(null);
            if (blockName == null) {
                CLIENT_LOG.warn("Received block update packet for unknown block: {}", record.getBlock());
                return;
            }
            if (blockName.equals("chest")) {
                writeTileEntity(chunk, "minecraft:chest", BlockEntityType.CHEST, pos);
            } else if (blockName.equals("trapped_chest")) {
                writeTileEntity(chunk, "minecraft:trapped_chest", BlockEntityType.TRAPPED_CHEST, pos);
            } else if (blockName.equals("ender_chest")) {
                writeTileEntity(chunk, "minecraft:ender_chest", BlockEntityType.ENDER_CHEST, pos);
            } else if (blockName.equals("enchanting_table")) {
                writeTileEntity(chunk, "minecraft:enchanting_table", BlockEntityType.ENCHANTING_TABLE, pos);
            }
            // todo: fill out for every BlockEntityType value?
        }
    }

    private void writeTileEntity(final Chunk chunk, final String blockName, final BlockEntityType type, final Vec3i position) {
        // todo: no idea if this compound tag is correct still
        final CompoundTag tileEntityTag = new CompoundTag(blockName, ImmutableMap.of(
            // there's probably more properties some tile entities need but this seems to work well enough
            "id", new StringTag("id", blockName),
            "x", new IntTag("x", position.getX()),
            "y", new IntTag("y", position.getY()),
            "z", new IntTag("z", position.getZ())
        ));
        Optional<BlockEntityInfo> foundTileEntity = chunk.blockEntities.stream()
            .filter(tileEntity -> tileEntity.getX() == position.getX() &&
                tileEntity.getY() == position.getY() &&
                tileEntity.getZ() == position.getZ())
            .findFirst();
        if (foundTileEntity.isPresent()) {
            foundTileEntity.get().setNbt(tileEntityTag);
        } else {
            chunk.blockEntities.add(new BlockEntityInfo(position.getX(),
                                                        position.getY(),
                                                        position.getZ(),
                                                        type,
                                                        tileEntityTag));
        }
    }

    public boolean handleChunkBiomes(final ClientboundChunksBiomesPacket packet) {
        return writeCache(() -> {
            for (ChunkBiomeData biomeData: packet.getChunkBiomeData()) {
                Chunk chunk = this.cache.get(chunkPosToLong(biomeData.getX(), biomeData.getZ()));
                if (chunk == this.cache.defaultReturnValue()) {
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
        });
    }

    public boolean handleLightUpdate(final ClientboundLightUpdatePacket packet) {
        return writeCache(() -> {
            Chunk chunk = this.cache.get(chunkPosToLong(packet.getX(), packet.getZ()));
            if (chunk == this.cache.defaultReturnValue()) {
                CLIENT_LOG.warn("Received light update packet for unknown chunk: {} {}", packet.getX(), packet.getZ());
                return false;
            } else {
                chunk.lightUpdateData = packet.getLightData();
            }
            return true;
        });
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
        return writeCache(() -> {
            int chunkX = packet.getPosition().getX() >> 4;
            int chunkZ = packet.getPosition().getZ() >> 4;
            final Chunk chunk = this.cache.get(chunkPosToLong(chunkX, chunkZ));
            if (chunk == this.cache.defaultReturnValue()) {
                CLIENT_LOG.warn("Received tile entity update packet for unknown chunk: {} {}", chunkX, chunkZ);
                return false;
            }
            final List<BlockEntityInfo> tileEntities = chunk.blockEntities;
            final Optional<BlockEntityInfo> existingTileEntity = tileEntities.stream()
                .filter(tileEntity -> tileEntity.getX() == packet.getPosition().getX() &&
                    tileEntity.getY() == packet.getPosition().getY() &&
                    tileEntity.getZ() == packet.getPosition().getZ())
                .findFirst();
            final CompoundTag packetNbt = packet.getNbt();
            if (packetNbt != null && !packetNbt.isEmpty()) {
                // ensure position is encoded in NBT
                // not sure if this is totally needed or not
                packetNbt.put(new IntTag("x", packet.getPosition().getX()));
                packetNbt.put(new IntTag("y", packet.getPosition().getY()));
                packetNbt.put(new IntTag("z", packet.getPosition().getZ()));
                existingTileEntity.ifPresentOrElse(
                    tileEntity -> tileEntity.setNbt(packetNbt),
                    () -> tileEntities.add(new BlockEntityInfo(packet.getPosition().getX(),
                                                               packet.getPosition().getY(),
                                                               packet.getPosition().getZ(),
                                                               packet.getType(),
                                                               packetNbt))
                );
            } else {
                existingTileEntity.ifPresent(tileEntities::remove);
            }
            return true;
        });
    }

    public <T> T readCache(final Supplier<T> executable) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    return executable.get();
                } catch (final Throwable e) {
                    CLIENT_LOG.error("Error reading chunk cache", e);
                } finally {
                    lock.readLock().unlock();
                }
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error reading chunk cache", e);
        }
        return null;
    }

    public <T> T writeCache(final Supplier<T> executable) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    return executable.get();
                } catch (final Throwable e) {
                    CLIENT_LOG.error("Error reading chunk cache", e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error reading chunk cache", e);
        }
        return null;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        try {
            consumer.accept(new ClientboundSetChunkCacheRadiusPacket(serverViewDistance));
            consumer.accept(new ClientboundSetChunkCacheCenterPacket(centerX, centerZ));
            readCache(() -> {
                this.cache.values().parallelStream()
                    .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                        chunk.x,
                        chunk.z,
                        chunk.serialize(CACHE.getChunkCache().getCodec()),
                        new CompoundTag("heightmap"), // todo: verify if we need to populate heightmaps
                        chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                        chunk.lightUpdateData))
                    .forEach(consumer);
                return true;
            });
        } catch (Exception e) {
            CLIENT_LOG.error("Error getting ChunkData packets from cache", e);
        }
        consumer.accept(new ClientboundSetDefaultSpawnPositionPacket(Vector3i.from(spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()), 0.0f));
        if (isRaining) {
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.START_RAIN, null));
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.RAIN_STRENGTH, new RainStrengthValue(this.rainStrength)));
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.THUNDER_STRENGTH, new ThunderStrengthValue(this.thunderStrength)));
        }
    }

    @Override
    public void reset(boolean full) {
        writeCache(() -> {
            this.cache.clear();
            this.spawnPosition = DEFAULT_SPAWN_POSITION;
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
            }
            return true;
        });
    }

    @Override
    public String getSendingMessage() {
        final AtomicReference<String> format = new AtomicReference<String>();
        readCache(() -> {
            format.set(String.format("Sending %d chunks, world spawn position [%d, %d, %d]",
                                     this.cache.size(),
                                     this.spawnPosition.getX(),
                                     this.spawnPosition.getY(),
                                     this.spawnPosition.getZ()));
            return true;
        });
        return format.get();
    }

    private static int longToChunkX(final long l) {
        return (int) (l & 4294967295L);
    }

    private static int longToChunkZ(final long l) {
        return (int) (l >> 32 & 4294967295L);
    }

    public void add(final ClientboundLevelChunkWithLightPacket p) {
        readCache(() -> {
            if (worldData == null) {
                CLIENT_LOG.error("Received chunk data packet while not in a dimension");
                return false;
            }
            int chunkX = p.getX();
            int chunkZ = p.getZ();
            byte[] data = p.getChunkData();
            ByteBuf buf = Unpooled.wrappedBuffer(data);
            int sectionsCount = getSectionsCount();
            Chunk existing = cache.get(chunkPosToLong(chunkX, chunkZ));
            Chunk chunk = existing;
            if (existing == cache.defaultReturnValue()) {
                chunk = new Chunk(chunkX,
                                  chunkZ,
                                  new ChunkSection[sectionsCount],
                                  sectionsCount,
                                  new ArrayList<>(
                                      List.of(p.getBlockEntities())),
                                  p.getLightData());
            }
            for (int i = 0; i < chunk.sectionsCount; i++) {
                chunk.sections[i] = readChunkSection(buf);
            }
            cache.put(chunkPosToLong(chunkX, chunkZ), chunk);
            return true;
        });
    }

    public ChunkSection readChunkSection(ByteBuf buf) throws UncheckedIOException {
        if (biomesEntryBitsSize == -1) {
            throw new IllegalStateException("Biome entry bits size is not set");
        }

        int blockCount = buf.readShort();
        DataPalette chunkPalette = codec
            .readDataPalette(buf, PaletteType.CHUNK, Shared.BLOCK_DATA_MANAGER.getBlockBitsPerEntry());
        DataPalette biomePalette = codec
            .readDataPalette(buf, PaletteType.BIOME, biomesEntryBitsSize);
        return new ChunkSection(blockCount, chunkPalette, biomePalette);
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
        return readCache(() -> {
            Chunk chunk = this.cache.get(chunkPosToLong(x, z));
            return (chunk == this.cache.defaultReturnValue()) ? null : chunk;
        });
    }

    // section for blockpos
    public ChunkSection getChunkSection(int x, int y, int z) {
        return readCache(() -> {
            Chunk chunk = get(x >> 4, z >> 4);
            if (chunk == null) return null;
            return chunk.sections[(y >> 4) - getMinSection()];
        });
    }

    public void remove(int x, int z) {
        writeCache(() -> this.cache.remove(chunkPosToLong(x, z)));
    }

    // reap any chunks we possibly didn't remove from the cache
    // dead chunks could occur due to race conditions, packet ordering, or bad server
    // doesn't need to be invoked frequently and this is not a condition that happens normally
    // i'm adding this because we are very memory constrained
    private void reapDeadChunks() {
        if (!Proxy.getInstance().isConnected()) return;
        final int playerX = ((int) CACHE.getPlayerCache().getX()) >> 4;
        final int playerZ = ((int) CACHE.getPlayerCache().getZ()) >> 4;
        writeCache(() -> {
            final long[] toRemove = cache.keySet().longStream()
                .filter(key -> distanceOutOfRange(playerX, playerZ, longToChunkX(key), longToChunkZ(key)))
                .toArray();
            for (final long l : toRemove) {
                cache.remove(l);
            }
            if (toRemove.length > 0) {
                CLIENT_LOG.warn("Reaped {} dead chunks", toRemove.length);
            }
            return true;
        });
    }

    private boolean distanceOutOfRange(final int x1, final int y1, final int x2, final int y2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) > maxDistanceExpected;
    }

    public void updateCurrentDimension(final ClientboundRespawnPacket packet) {
        this.currentDimension = dimensionRegistry.get(packet.getDimension());
        this.worldData = new WorldData(currentDimension.dimensionName, // todo: verify if this is even relevant
                                       currentDimension.dimensionName,
                                       packet.getHashedSeed(),
                                       packet.isDebug(),
                                       packet.isFlat());
    }
}
