package com.zenith.cache.data.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.TileEntity;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.data.game.world.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.world.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.ImmutableMap;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.server.ServerConnection;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ChunkCache implements CachedData, BiFunction<Column, Column, Column> {
    private static final Position DEFAULT_SPAWN_POSITION = new Position(8, 64, 8);
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    @Getter
    @Setter
    protected Position spawnPosition = DEFAULT_SPAWN_POSITION;
    @Getter
    @Setter
    private boolean isRaining = false;
    @Getter
    @Setter
    private float rainStrength = 0f;
    @Getter
    @Setter
    private float thunderStrength = 0f;
    protected final Long2ObjectOpenHashMap<Column> cache = new Long2ObjectOpenHashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public ChunkCache() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::reapDeadChunks, 5L, 5L, TimeUnit.MINUTES);
    }

    public static void sync() {
        final ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(currentPlayer)) {
            try {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    CACHE.getChunkCache().cache.values().parallelStream()
                            .map(ServerChunkDataPacket::new)
                            .forEach(currentPlayer::send);
                    lock.readLock().unlock();
                }
            } catch (final Exception e) {
                CLIENT_LOG.error("Error sending chunk data", e);
            }
        }
    }

    /**
     * @deprecated do not call this directly!
     */
    @Override
    @Deprecated
    public Column apply(@NonNull Column existing, @NonNull Column add) {
        try {
            Chunk[] chunks = existing.getChunks();
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                for (int chunkY = 0; chunkY < 16; chunkY++) {
                    Chunk addChunk = add.getChunks()[chunkY];
                    if (addChunk == null) {
                        continue;
                    } else if (add.hasSkylight()) {
                        chunks[chunkY] = addChunk;
                    } else {
                        chunks[chunkY] = new Chunk(addChunk.getBlocks(), addChunk.getBlockLight(), chunks[chunkY] == null ? null : chunks[chunkY].getSkyLight());
                    }
                }
                lock.writeLock().unlock();
            }
            return new Column(
                    add.getX(), add.getZ(),
                    chunks,
                    add.hasBiomeData() ? add.getBiomeData() : existing.getBiomeData(),
                    add.getTileEntities());
        } catch (final Exception e) {
            CLIENT_LOG.error("Error merging chunk data", e);
        }
        return null;
    }

    private static long chunkPosToLong(final int x, final int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public boolean updateBlock(final BlockChangeRecord record) {
        try {
            final Position pos = record.getPosition();
            if (pos.getY() < 0 || pos.getY() >= 256) {
                return true;
            }
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                Column column = get(pos.getX() >> 4, pos.getZ() >> 4);
                if (column != null) {
                    Chunk chunk = column.getChunks()[pos.getY() >> 4];
                    if (chunk == null) {
                        chunk = column.getChunks()[pos.getY() >> 4] = new Chunk(column.hasSkylight());
                    }
                    lock.readLock().unlock();
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        chunk.getBlocks().set(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, record.getBlock());
                        lock.writeLock().unlock();
                    }
                    handleBlockUpdateTileEntity(record, pos, column);
                } else {
                    lock.readLock().unlock();
                }
            }

        } catch (final Exception e) {
            CLIENT_LOG.error("Error applying block update", e);
            return false;
        }

        return true;
    }

    // update any tile entities implicitly affected by this block update
    // server doesn't always send us tile entity update packets and relies on logic in client
    private void handleBlockUpdateTileEntity(BlockChangeRecord record, Position pos, Column column) {
        if (record.getBlock().getId() == 0 && record.getBlock().getData() == 0) {
            try {
                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    final List<TileEntity> tileEntitiesList = column.getTileEntities();
                    final Optional<TileEntity> foundTileEntity = tileEntitiesList.stream()
                            .filter(tileEntity -> tileEntity.getPosition().equals(pos))
                            .findFirst();
                    foundTileEntity.ifPresent(tileEntitiesList::remove);
                    lock.writeLock().unlock();
                }
            } catch (Exception e) {
                CLIENT_LOG.error("Error removing tile entity", e);
            }
        } else {
            // if we don't create a tile entity for certain blocks they render with no texture for some reason
            if (record.getBlock().getId() == 54) {
                writeTileEntity(column, "minecraft:chest", record.getPosition());
            } else if (record.getBlock().getId() == 146) {
                writeTileEntity(column, "minecraft:trapped_chest", record.getPosition());
            } else if (record.getBlock().getId() == 130) {
                writeTileEntity(column, "minecraft:ender_chest", record.getPosition());
            } else if (record.getBlock().getId() == 116) {
                writeTileEntity(column, "minecraft:enchanting_table", record.getPosition());
            }
        }
    }

    public boolean multiBlockUpdate(final ServerMultiBlockChangePacket packet) {
        for (BlockChangeRecord record : packet.getRecords()) {
            updateBlock(record);
        }
        return true;
    }

    public boolean updateBlock(ServerBlockChangePacket packet) {
        return updateBlock(packet.getRecord());
    }

    private void writeTileEntity(final Column column, final String tileEntityId, final Position position) {
        final CompoundTag tileEntityTag = new CompoundTag(tileEntityId, ImmutableMap.of(
                // there's probably more properties some tile entities need but this seems to work well enough
                "id", new StringTag("id", tileEntityId),
                "x", new IntTag("x", position.getX()),
                "y", new IntTag("y", position.getY()),
                "z", new IntTag("z", position.getZ())
        ));
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                final Optional<TileEntity> foundTileEntity = column.getTileEntities()
                        .stream()
                        .filter(tileEntity -> tileEntity.getPosition().equals(position))
                        .findFirst();
                if (foundTileEntity.isPresent()) {
                    foundTileEntity.get().setCompoundTag(tileEntityTag);
                } else {
                    column.getTileEntities().add(new TileEntity(position, tileEntityTag));
                }
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error writing tile entity", e);
        }
    }

    public boolean updateTileEntity(final ServerUpdateTileEntityPacket packet) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                final Column column = get(packet.getPosition().getX() >> 4, packet.getPosition().getZ() >> 4);
                if (isNull(column)) {
                    return false;
                }
                final List<TileEntity> tileEntities = column.getTileEntities();
                final Optional<TileEntity> existingTileEntity = tileEntities.stream()
                        .filter(tileEntity -> tileEntity.getPosition().equals(packet.getPosition()))
                        .findFirst();
                final CompoundTag packetNbt = packet.getNBT();
                if (packetNbt != null && !packetNbt.isEmpty()) {
                    // ensure position is encoded in NBT
                    // not sure if this is totally needed or not
                    packetNbt.put(new IntTag("x", packet.getPosition().getX()));
                    packetNbt.put(new IntTag("y", packet.getPosition().getY()));
                    packetNbt.put(new IntTag("z", packet.getPosition().getZ()));
                    if (existingTileEntity.isPresent()) {
                        existingTileEntity.get().setCompoundTag(packetNbt);
                    } else {
                        tileEntities.add(new TileEntity(packet.getPosition(), packetNbt));
                    }
                } else {
                    existingTileEntity.ifPresent(tileEntities::remove);
                }
                lock.writeLock().unlock();
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
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                this.cache.values().parallelStream()
                        .map(ServerChunkDataPacket::new)
                        .forEach(consumer);
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            CLIENT_LOG.error("Error getting ChunkData packets from cache", e);
        }
        consumer.accept(new ServerSpawnPositionPacket(spawnPosition));
        if (isRaining) {
            consumer.accept(new ServerNotifyClientPacket(ClientNotification.START_RAIN, null));
            consumer.accept(new ServerNotifyClientPacket(ClientNotification.RAIN_STRENGTH, new RainStrengthValue(this.rainStrength)));
            consumer.accept(new ServerNotifyClientPacket(ClientNotification.THUNDER_STRENGTH, new ThunderStrengthValue(this.thunderStrength)));
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

    public void add(@NonNull Column column) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                this.cache.merge(chunkPosToLong(column.getX(), column.getZ()), column, this);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to merge chunk column", e);
        }
    }

    public Column get(int x, int z) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                Column column = this.cache.get(chunkPosToLong(x, z));
                lock.readLock().unlock();
                return column;
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
}
