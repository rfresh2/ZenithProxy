package com.zenith.cache.data.chunk;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.*;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.ImmutableMap;
import com.zenith.Proxy;
import com.zenith.Shared;
import com.zenith.cache.CachedData;
import com.zenith.network.server.ServerConnection;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;
import static java.util.Objects.nonNull;

@Getter
@Setter
public class ChunkCache implements CachedData {
    private static final Vector3i DEFAULT_SPAWN_POSITION = Vector3i.from(8, 64, 8);
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    protected Vector3i spawnPosition = DEFAULT_SPAWN_POSITION;
    // todo: consider moving weather to a separate cache object
    private boolean isRaining = false;
    private float rainStrength = 0f;
    private float thunderStrength = 0f;
    private int renderDistance = 25;
    protected final Long2ObjectOpenHashMap<Chunk> cache = new Long2ObjectOpenHashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    // todo: clear on cache reset
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
//        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::reapDeadChunks, 5L, 5L, TimeUnit.MINUTES);
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
        if (nonNull(currentPlayer)) {
            try {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    CACHE.getChunkCache().cache.values().parallelStream()
                            .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                                    chunk.x,
                                    chunk.z,
                                    chunk.serialize(CACHE.getChunkCache().getCodec()),
                                    new CompoundTag("heightmap"), // todo: verify if we need to populate heightmaps
                                    chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                                    chunk.lightUpdateData))
                            .forEach(currentPlayer::send);
                    lock.readLock().unlock();
                }
            } catch (final Exception e) {
                CLIENT_LOG.error("Error sending chunk data", e);
            }
        }
    }

    private static long chunkPosToLong(final int x, final int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public boolean updateBlock(final @NonNull BlockChangeEntry record) {
        try {
            Vector3i pos = record.getPosition();
//            if (pos.getY() < 0 || pos.getY() >= 256) {
//                return true;
//            }
//            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
//                ChunkSection column = get(pos.getX() >> 4, pos.getZ() >> 4);
//                if (column != null) {
//                    Chunk chunk = column.getChunks()[pos.getY() >> 4];
//                    if (chunk == null) {
//                        chunk = new Chunk(column.hasSkylight());
//                    }
//                    lock.readLock().unlock();
//                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
//                        chunk.getBlocks().set(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, record.getBlock());
//                        lock.writeLock().unlock();
//                    }
//                    handleBlockUpdateTileEntity(record, pos, column);
//                } else {
//                    lock.readLock().unlock();
//                }
//            }

        } catch (final Exception e) {
            CLIENT_LOG.error("Error applying block update", e);
            return false;
        }

        return true;
    }

    // update any tile entities implicitly affected by this block update
    // server doesn't always send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateTileEntity(BlockChangeEntry record, Vector3i pos, ChunkSection column) {
//        if (record.getBlock().getId() == 0 && record.getBlock().getData() == 0) {
//            try {
//                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
//                    final List<TileEntity> tileEntitiesList = column.getTileEntities();
//                    final Optional<TileEntity> foundTileEntity = tileEntitiesList.stream()
//                            .filter(tileEntity -> tileEntity.getPosition().equals(pos))
//                            .findFirst();
//                    foundTileEntity.ifPresent(tileEntitiesList::remove);
//                    lock.writeLock().unlock();
//                }
//            } catch (Exception e) {
//                CLIENT_LOG.error("Error removing tile entity", e);
//            }
//        } else {
//            // if we don't create a tile entity for certain blocks they render with no texture for some reason
//            if (record.getBlock().getId() == 54) {
//                writeTileEntity(column, "minecraft:chest", record.getPosition());
//            } else if (record.getBlock().getId() == 146) {
//                writeTileEntity(column, "minecraft:trapped_chest", record.getPosition());
//            } else if (record.getBlock().getId() == 130) {
//                writeTileEntity(column, "minecraft:ender_chest", record.getPosition());
//            } else if (record.getBlock().getId() == 116) {
//                writeTileEntity(column, "minecraft:enchanting_table", record.getPosition());
//            }
//        }
    }

    public void lightUpdate(final ClientboundLightUpdatePacket packet) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                Chunk chunk = this.cache.get(chunkPosToLong(packet.getX(), packet.getZ()));
                if (chunk == this.cache.defaultReturnValue()) {
                    CLIENT_LOG.warn("Received light update packet for unknown chunk: {} {}", packet.getX(), packet.getZ());
                } else {
                    chunk.lightUpdateData = packet.getLightData();
                }
                lock.writeLock().unlock();
            }

        } catch (final Exception e) {
            CLIENT_LOG.info("Error applying light update at chunk: {} {}", packet.getX(), packet.getZ(), e);
        }
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

    private void writeTileEntity(final ChunkSection column, final String tileEntityId, final Vector3i position) {
        final CompoundTag tileEntityTag = new CompoundTag(tileEntityId, ImmutableMap.of(
                // there's probably more properties some tile entities need but this seems to work well enough
                "id", new StringTag("id", tileEntityId),
                "x", new IntTag("x", position.getX()),
                "y", new IntTag("y", position.getY()),
                "z", new IntTag("z", position.getZ())
        ));
        try {
//            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
//                final Optional<TileEntity> foundTileEntity = column.getTileEntities()
//                        .stream()
//                        .filter(tileEntity -> tileEntity.getPosition().equals(position))
//                        .findFirst();
//                if (foundTileEntity.isPresent()) {
//                    foundTileEntity.get().setCompoundTag(tileEntityTag);
//                } else {
//                    column.getTileEntities().add(new TileEntity(position, tileEntityTag));
//                }
//                lock.writeLock().unlock();
//            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error writing tile entity", e);
        }
    }

    public boolean updateTileEntity(final ClientboundBlockEntityDataPacket packet) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
//                final ChunkSection column = get(packet.getPosition().getX() >> 4, packet.getPosition().getZ() >> 4);
//                if (isNull(column)) {
//                    lock.readLock().unlock();
//                    return false;
//                }
                lock.readLock().unlock();
//                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
//                    final List<TileEntity> tileEntities = column.getTileEntities();
//                    final Optional<TileEntity> existingTileEntity = tileEntities.stream()
//                            .filter(tileEntity -> tileEntity.getPosition().equals(packet.getPosition()))
//                            .findFirst();
//                    final CompoundTag packetNbt = packet.getNBT();
//                    if (packetNbt != null && !packetNbt.isEmpty()) {
//                        // ensure position is encoded in NBT
//                        // not sure if this is totally needed or not
//                        packetNbt.put(new IntTag("x", packet.getPosition().getX()));
//                        packetNbt.put(new IntTag("y", packet.getPosition().getY()));
//                        packetNbt.put(new IntTag("z", packet.getPosition().getZ()));
//                        existingTileEntity.ifPresentOrElse(
//                                tileEntity -> tileEntity.setCompoundTag(packetNbt),
//                                () -> tileEntities.add(new TileEntity(packet.getPosition(), packetNbt))
//                        );
//                    } else {
//                        existingTileEntity.ifPresent(tileEntities::remove);
//                    }
//                    lock.writeLock().unlock();
//                }
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error applying tile entity update", e);
            return false;
        }
        return true;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        try {
            consumer.accept(new ClientboundSetChunkCacheRadiusPacket(serverViewDistance));
            consumer.accept(new ClientboundSetChunkCacheCenterPacket(centerX, centerZ));
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                this.cache.values().parallelStream()
                    .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                        chunk.x,
                        chunk.z,
                        chunk.serialize(CACHE.getChunkCache().getCodec()),
                        new CompoundTag("heightmap"), // todo: verify if we need to populate heightmaps
                        chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                        chunk.lightUpdateData))
                    .forEach(consumer);
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            CLIENT_LOG.error("Error getting ChunkData packets from cache", e);
        }
        consumer.accept(new ClientboundSetDefaultSpawnPositionPacket(spawnPosition, 0.0f));
        if (isRaining) {
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.START_RAIN, null));
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.RAIN_STRENGTH, new RainStrengthValue(this.rainStrength)));
//            consumer.accept(new ServerNotifyClientPacket(ClientNotification.THUNDER_STRENGTH, new ThunderStrengthValue(this.thunderStrength)));
        }
    }

    @Override
    public void reset(boolean full) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                this.cache.clear();
                lock.writeLock().unlock();
                this.spawnPosition = DEFAULT_SPAWN_POSITION;
                this.isRaining = false;
                this.thunderStrength = 0.0f;
                this.rainStrength = 0.0f;
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire write lock", e);
        }

    }

    @Override
    public String getSendingMessage() {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                String format = String.format("Sending %d chunks, world spawn position [%d, %d, %d]",
                        this.cache.size(),
                        this.spawnPosition.getX(),
                        this.spawnPosition.getY(),
                        this.spawnPosition.getZ());
                lock.readLock().unlock();
                return format;
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire read lock", e);
        }
        return null;
    }

    private static int longToChunkX(final long l) {
        return (int) (l & 4294967295L);
    }

    private static int longToChunkZ(final long l) {
        return (int) (l >> 32 & 4294967295L);
    }

    public void add(final ClientboundLevelChunkWithLightPacket p) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (worldData == null) {
                        CLIENT_LOG.error("Received chunk data packet while not in a dimension");
                        return;
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
                    // todo: verify we don't need a special merge function
                    //  is it possible for servers to send partial chunk data here still?
                    for (int i = 0; i < chunk.sectionsCount; i++) {
                        chunk.sections[i] = readChunkSection(buf);
                    }
                    cache.put(chunkPosToLong(chunkX, chunkZ), chunk);
                    CLIENT_LOG.info("Cached chunk {} {}", chunkX, chunkZ);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire write lock", e);
        }
    }

    public ChunkSection readChunkSection(ByteBuf buf) throws UncheckedIOException {
        if (biomesEntryBitsSize == -1) {
            throw new IllegalStateException("Biome entry bits size is not set");
        }

        int blockCount = buf.readShort();
        DataPalette chunkPalette = codec.readDataPalette(buf,
                                                         PaletteType.CHUNK,
                                                         Shared.BLOCK_DATA_MANAGER.getBlockBitsPerEntry());
        DataPalette biomePalette = codec.readDataPalette(buf,
                                                         PaletteType.BIOME,
                                                         biomesEntryBitsSize);
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
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                Chunk chunk = this.cache.get(chunkPosToLong(x, z));
                lock.readLock().unlock();
                return chunk;
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire read lock", e);
        }
        return null;
    }

    public void remove(int x, int z) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                this.cache.remove(chunkPosToLong(x, z));
                CLIENT_LOG.info("Removed cached chunk: {} {}", x, z);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire write lock", e);
        }
    }

    // reap any chunks we possibly didn't remove from the cache
    // dead chunks could occur due to race conditions, packet ordering, or bad server
    // doesn't need to be invoked frequently and this is not a condition that happens normally
    // i'm adding this because we are very memory constrained
    private void reapDeadChunks() {
        if (!Proxy.getInstance().isConnected()) return;
        final int playerX = ((int) CACHE.getPlayerCache().getX()) >> 4;
        final int playerZ = ((int) CACHE.getPlayerCache().getZ()) >> 4;
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                final long[] toRemove = cache.keySet().longStream()
                        .filter(key -> distanceOutOfRange(playerX, playerZ, longToChunkX(key), longToChunkZ(key)))
                        .toArray();
                lock.readLock().unlock();
                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (final long l : toRemove) {
                        cache.remove(l);
                    }
                    lock.writeLock().unlock();
                }
                if (toRemove.length > 0) {
                    CLIENT_LOG.warn("Reaped {} dead chunks", toRemove.length);
                }
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to acquire read lock", e);
        }
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
