package com.zenith.cache.data.chunk;

import com.viaversion.nbt.mini.MNBT;
import com.viaversion.nbt.mini.MNBTWriter;
import com.zenith.Proxy;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import com.zenith.mc.dimension.DimensionData;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.network.server.ServerSession;
import com.zenith.util.BrandSerializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Data;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkBiomeData;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static com.zenith.Globals.*;
import static com.zenith.cache.data.chunk.Chunk.chunkPosToLong;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;

@Data
@Accessors(chain = true)
public class ChunkCache implements CachedData {
    // todo: consider moving weather to a separate cache object
    private boolean isRaining = false;
    private float rainStrength = 0f;
    private float thunderStrength = 0f;
    // iterators over this map are not thread safe.
    // to do iteration, copy the key or value set into a new list, then iterate over that copied list.
    // trade-off: faster and lower memory lookups (compared to ConcurrentHashMap), but slower and more memory intensive iteration
    protected final Long2ObjectOpenHashMap<Chunk> cache = new Long2ObjectOpenHashMap<>(81, 0.5f);
    protected @Nullable DimensionData currentDimension = null;
    protected List<Key> worldNames = new ArrayList<>();
    protected int serverViewDistance = -1;
    protected int serverSimulationDistance = -1;
    protected int centerX;
    protected int centerZ;
    protected int dimensionType;
    protected Key worldName;
    protected long hashedSeed;
    protected boolean debug;
    protected boolean flat;
    protected int seaLevel;
    protected BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();

    // todo: also cache world border size changes
    //  doesn't particularly matter on 2b2t tho
    protected WorldBorderData worldBorderData = WorldBorderData.DEFAULT;
    protected WorldTimeData worldTimeData;
    protected byte[] serverBrand = null;

    public void setCurrentWorld(final int dimensionId, final Key worldName, long hashedSeed, boolean debug, boolean flat) {
        this.dimensionType = dimensionId;
        this.worldName = worldName;
        this.hashedSeed = hashedSeed;
        this.debug = debug;
        this.flat = flat;
        var worldDimension = DimensionRegistry.REGISTRY.get(dimensionId);
        if (worldDimension == null) {
            CACHE_LOG.warn("Received unknown dimension ID: {}", dimensionId);
            if (DimensionRegistry.REGISTRY.size() > 0) {
                worldDimension = DimensionRegistry.REGISTRY.get(0);
                if (worldDimension == null) {
                    throw new RuntimeException("No dimensions in registry");
                }
            } else {
                throw new RuntimeException("No dimensions in registry");
            }
        }
        this.currentDimension = worldDimension;
        CACHE_LOG.debug("Updated current world to {}", worldName);
        CACHE_LOG.debug("Current dimension: {}", currentDimension.name());
    }

    public static void sync() {
        final ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer == null) return;
        currentPlayer.sendAsync(new ClientboundChunkBatchStartPacket());
        var chunks = new ArrayList<>(CACHE.getChunkCache().cache.values());
        chunks.stream()
            .map(chunk -> new ClientboundLevelChunkWithLightPacket(
                chunk.x,
                chunk.z,
                chunk.sections,
                chunk.getHeightMap(),
                chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                chunk.lightUpdateData)
            )
            .forEach(currentPlayer::sendAsync);
        CACHE_LOG.info("Syncing {} chunks to current player", chunks.size());
        currentPlayer.sendAsync(new ClientboundChunkBatchFinishedPacket(chunks.size()));
    }

    public boolean isChunkLoaded(final int x, final int z) {
        return cache.containsKey(chunkPosToLong(x, z));
    }

    public boolean updateBlock(final int x, final int y, final int z, final int blockStateId) {
        final var chunk = get(x >> 4, z >> 4);
        if (chunk != null) {
            var chunkSection = chunk.getChunkSection(y);
            if (chunkSection == null) {
                var palettedWorldState = Proxy.getInstance().getClient().getPalettedWorldState();
                chunkSection = new ChunkSection(0,
                    DataPalette.createForChunk(palettedWorldState),
                    DataPalette.createForBiome(palettedWorldState)
                );
            }
            // relative positions in the chunk
            int relativeX = x & 15;
            int relativeY = y & 15;
            int relativeZ = z & 15;
            int currentBlockId = chunkSection.getBlock(relativeX, relativeY, relativeZ);
            chunkSection.setBlock(relativeX, relativeY, relativeZ, blockStateId);
            handleBlockUpdateBlockEntity(blockStateId, relativeX, y, relativeZ, chunk);
            if (blockStatePredictionHandler.isPredicting()) {
                blockStatePredictionHandler.retainKnownServerState(x, y, z, currentBlockId, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ());
            }
        } else {
            CLIENT_LOG.debug("Block update packet in unknown chunk: {} {}", x >> 4, z >> 4);
            return false;
        }
        return true;
    }

    public boolean updateBlock(final @NonNull BlockChangeEntry record) {
        try {
            if (record.getY() < currentDimension.minY() || record.getY() >= currentDimension.minY() + currentDimension.height()) {
                // certain client modules might cause the server to send us block updates out of bounds if we send illegal dig packets
                // instead of causing a retry of the block update, just return true and ignore it
                return true;
            }
            if (blockStatePredictionHandler.updateKnownServerState(record.getX(), record.getY(), record.getZ(), record.getBlock())) {
                return true;
            }
            return updateBlock(record.getX(), record.getY(), record.getZ(), record.getBlock());
        } catch (final Exception e) {
            CLIENT_LOG.debug("Error updating block", e);
            return false;
        }
    }

    // update any block entities implicitly affected by this block update
    // server doesn't send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateBlockEntity(int blockStateId, int relativeX, int y, int relativeZ, Chunk chunk) {
        var block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
        if (block == null) {
            CLIENT_LOG.debug("Received block update packet for unknown block id: {}", blockStateId);
            return;
        }
        if (block.isAir()) {
            chunk.blockEntities.removeIf(tileEntity -> tileEntity.getX() == relativeX && tileEntity.getY() == y && tileEntity.getZ() == relativeZ);
        } else {
            if (block.blockEntityType() != null && !chunkContainsBlockEntityTypeAtPos(chunk, relativeX, y, relativeZ, block.blockEntityType())) {
                writeBlockEntity(chunk, block.name(), block.blockEntityType(), relativeX, y, relativeZ);
            }
        }
    }

    private void writeBlockEntity(final Chunk chunk, final String blockName, final BlockEntityType type, final int relativeX, final int y, final int relativeZ) {
        final MNBT nbt = getBlockEntityNBT(blockName, (chunk.getX() * 16) + relativeX, y, (chunk.getZ() * 16) + relativeZ);
        updateOrAddBlockEntity(chunk, relativeX, y, relativeZ, type, nbt);
    }

    private MNBT getBlockEntityNBT(final String blockName, final int x, final int y, final int z) {
        try (MNBTWriter writer = new MNBTWriter()) {
            writer.writeStartTag();
            writer.writeStringTag("id", "minecraft:" + blockName);
            writer.writeIntTag("x", x);
            writer.writeIntTag("y", y);
            writer.writeIntTag("z", z);
            writer.writeEndTag();
            return writer.toMNBT();
        } catch (final Throwable e) {
            throw new RuntimeException("Error writing block entity: " + blockName, e);
        }
    }

    private void updateOrAddBlockEntity(final Chunk chunk, final int relativeX, final int y, final int relativeZ, final BlockEntityType type, final MNBT nbt) {
        synchronized (chunk.blockEntities) {
            for (BlockEntityInfo tileEntity : chunk.blockEntities) {
                if (tileEntity.getX() == relativeX
                    && tileEntity.getY() == y
                    && tileEntity.getZ() == relativeZ) {
                    tileEntity.setNbt(nbt);
                    return;
                }
            }
            chunk.blockEntities.add(new BlockEntityInfo(relativeX, y, relativeZ, type, nbt));
        }
    }

    private boolean chunkContainsBlockEntityTypeAtPos(final Chunk chunk, final int relativeX, final int y, final int relativeZ, final BlockEntityType type) {
        synchronized (chunk.blockEntities) {
            for (BlockEntityInfo tileEntity : chunk.blockEntities) {
                if (tileEntity.getType() == type
                    && tileEntity.getX() == relativeX
                    && tileEntity.getY() == y
                    && tileEntity.getZ() == relativeZ) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleChunkBiomes(final ClientboundChunksBiomesPacket packet) {
        final var chunkBiomeData = packet.getChunkBiomeData();
        for (int i = 0; i < chunkBiomeData.size(); i++) {
            final ChunkBiomeData biomeData = chunkBiomeData.get(i);
            Chunk chunk = get(biomeData.getX(), biomeData.getZ());
            if (chunk == null) {
                CLIENT_LOG.warn("Received chunk biomes packet for unknown chunk: {} {}",
                                biomeData.getX(),
                                biomeData.getZ());
                return false;
            } else {
                for (int j = 0; j < chunk.getSections().length; j++) {
                    DataPalette biomesData = biomeData.getPalettes()[j];
                    chunk.sections[j].setBiomeData(biomesData);
                }
            }
        }
        return true;
    }

    public boolean handleLightUpdate(final ClientboundLightUpdatePacket packet) {
        if (CONFIG.debug.server.cache.fullbrightChunkSkylight) return true;
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
        final var chunk = get(chunkX, chunkZ);
        if (chunk == null) return false;
        // when we place certain tile entities like beds, the server sends us a block entity update packet with empty nbt
        //  wiki.vg says this should mean the tile entity gets removed, however that doesn't seem to be correct
        updateOrAddBlockEntity(chunk, packet.getX() & 15, packet.getY(), packet.getZ() & 15, packet.getType(), packet.getNbt());
        return true;
    }

    public byte @Nullable [] getServerBrandRaw() {
        return serverBrand;
    }

    public byte[] getServerBrand() {
        return serverBrand == null
            ? BrandSerializer.defaultBrand()
            : BrandSerializer.appendBrand(serverBrand);
    }

    private static final byte[] fullBrightSkyLightData;
    static {
        fullBrightSkyLightData = new byte[2048];
        for (int j = 0; j < 2048; j++) {
            fullBrightSkyLightData[j] = (byte) 0b11111111;
        }
    }

    public LightUpdateData createFullBrightLightData(LightUpdateData lightData, int sectionCount) {
        var sectionPlusAboveBelowCount = sectionCount + 2;
        var skylightMaskSet = new BitSet(sectionPlusAboveBelowCount);
        skylightMaskSet.set(0, sectionPlusAboveBelowCount);
        // leave all empty
        var emptySkyLightMask = new BitSet(sectionPlusAboveBelowCount);
        List<byte[]> skyUpdates = new ArrayList<>(sectionPlusAboveBelowCount);
        for (int i = 0; i < sectionPlusAboveBelowCount; i++) {
            skyUpdates.add(fullBrightSkyLightData);
        }
        long[] lightMask = skylightMaskSet.toLongArray();
        long[] emptyLightMask = emptySkyLightMask.toLongArray();
        return new LightUpdateData(
            lightMask,
            CONFIG.debug.server.cache.fullbrightChunkBlocklight
                ? lightMask
                : lightData.getBlockYMask(),
            emptyLightMask,
            CONFIG.debug.server.cache.fullbrightChunkBlocklight
                ? emptyLightMask
                : lightData.getEmptyBlockYMask(),
            skyUpdates,
            CONFIG.debug.server.cache.fullbrightChunkBlocklight
                ? skyUpdates
                : lightData.getBlockUpdates()
        );
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer, final @NonNull TcpSession session) {
        try {
            final var brandBytes = getServerBrand();
            consumer.accept(new ClientboundCustomPayloadPacket(Key.key("minecraft", "brand"), brandBytes));
            consumer.accept(new ClientboundInitializeBorderPacket(
                worldBorderData.getCenterX(),
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
            // yucky extra mem copy but this is needed so we can safely iterate across threads
            // we could wrap access inside a try catch and hope for the best, but this shouldn't be called too often anyway
            var chunks = new ArrayList<>(this.cache.values());
            chunks.sort(Comparator.comparingInt(chunk -> Math.abs(chunk.x - centerX) + Math.abs(chunk.z - centerZ)));
            for (int i = 0; i < chunks.size(); i++) {
                var chunk = chunks.get(i);
                consumer.accept(new ClientboundLevelChunkWithLightPacket(
                    chunk.x,
                    chunk.z,
                    chunk.sections,
                    chunk.getHeightMap(),
                    chunk.blockEntities.toArray(new BlockEntityInfo[0]),
                    chunk.lightUpdateData
                ));
            }
            consumer.accept(new ClientboundChunkBatchFinishedPacket(this.cache.size()));
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
    public void reset(CacheResetType type) {
        // chunk reset, i.e. from changing worlds
        this.cache.clear();
        this.isRaining = false;
        this.thunderStrength = 0.0f;
        this.rainStrength = 0.0f;
        this.blockStatePredictionHandler = new BlockStatePredictionHandler();
        if (type == CacheResetType.FULL) {
            this.dimensionType = 0;
            worldNames = asList(Key.key("minecraft:overworld"), Key.key("minecraft:the_nether"), Key.key("minecraft:the_end"));
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

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d chunks", this.cache.size());
    }

    public void add(final ClientboundLevelChunkWithLightPacket p) {
        final var chunkX = p.getX();
        final var chunkZ = p.getZ();
        var pos = chunkPosToLong(chunkX, chunkZ);
        // todo: check if chunk is outside range like vanilla?
        //  Math.abs(x - this.viewCenterX) <= this.chunkRadius && Math.abs(z - this.viewCenterZ) <= this.chunkRadius
        var blockEntities = synchronizedList(newArrayList(p.getBlockEntities()));
        var chunk = new Chunk(
            chunkX,
            chunkZ,
            p.getSections(),
            getMaxSection(),
            getMinSection(),
            blockEntities,
            CONFIG.debug.server.cache.fullbrightChunkSkylight
                ? createFullBrightLightData(p.getLightData(), p.getSections().length)
                : p.getLightData()
        );
        this.cache.put(pos, chunk);
    }

    public int getSectionsCount() {
        var dim = currentDimension;
        return dim != null ? dim.sectionCount() : 1;
    }

    public int getMaxSection() {
        var dim = currentDimension;
        return dim != null ? dim.maxSection() : 1;
    }

    public int getMinSection() {
        var dim = currentDimension;
        return dim != null ? dim.minSection() : 0;
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

    public void updateCurrentDimension(final ClientboundRespawnPacket packet) {
        PlayerSpawnInfo info = packet.getCommonPlayerSpawnInfo();
        CACHE_LOG.debug("Updating current dimension to: {}", info.getDimension());
        DimensionData newDim = DimensionRegistry.REGISTRY.get(info.getDimension());
        if (newDim == null) {
            CACHE_LOG.error("Respawn packet tried updating dimension to unregistered dimension: {}", info.getDimension());
            CACHE_LOG.error("Things are going to break...");
        } else {
            this.currentDimension = newDim;
            this.worldName = Key.key("minecraft", currentDimension.name());
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
