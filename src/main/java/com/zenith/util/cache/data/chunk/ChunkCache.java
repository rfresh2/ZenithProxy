/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.util.cache.data.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import net.daporkchop.lib.math.vector.Vec2i;
import com.zenith.util.cache.CachedData;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

/**
 * @author DaPorkchop_
 */
public class ChunkCache implements CachedData, BiFunction<Column, Column, Column> {
    protected final Map<Vec2i, Column> cache = new ConcurrentHashMap<>();
    protected final Map<Vec3i, ServerBlockChangePacket> blockUpdates = new ConcurrentHashMap<>();
    protected final Map<Vec3i, ServerUpdateTileEntityPacket> tileEntityUpdates = new ConcurrentHashMap<>();

    public void add(@NonNull Column column) {
        synchronized (this) {
            this.cache.merge(Vec2i.of(column.getX(), column.getZ()), column, this);
            CACHE_LOG.debug("Cached chunk (%d, %d)", column.getX(), column.getZ());
        }
    }

    /**
     * @deprecated do not call this directly!
     */
    @Override
    @Deprecated
    public Column apply(@NonNull Column existing, @NonNull Column add) {
        CACHE_LOG.debug("Chunk (%d, %d) is already cached, merging with existing", add.getX(), add.getZ());
        Chunk[] chunks = existing.getChunks().clone();
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

        return new Column(
                add.getX(), add.getZ(),
                chunks,
                add.hasBiomeData() ? add.getBiomeData() : existing.getBiomeData(),
                add.getTileEntities());
    }

    public Column get(int x, int z) {
        return this.cache.get(Vec2i.of(x, z));
    }

    public void remove(int x, int z) {
        synchronized (this) {
            CACHE_LOG.debug("Server telling us to uncache chunk (%d, %d)", x, z);
            if (this.cache.remove(Vec2i.of(x, z)) == null) {
                CACHE_LOG.warn("Could not remove column (%d, %d)! this is probably a server issue", x, z);
            }

            final int xLow = x * 16;
            final int xHigh = xLow + 16; //exclusive
            final int zLow = z * 16;
            final int zHigh = zLow + 16; //exclusive

            List<Vec3i> blockUpdateKeysToRemove = this.blockUpdates.keySet().stream()
                    .filter(k -> (k.x() >= xLow && k.x() < xHigh)
                            && (k.z() >= zLow && k.z() < zHigh))
                    .collect(Collectors.toList());
            for (Vec3i key : blockUpdateKeysToRemove) {
                this.blockUpdates.remove(key);
            }
            List<Vec3i> tileEntityUpdateKeysToRemove = this.tileEntityUpdates.keySet().stream()
                    .filter(k -> (k.x() >= xLow && k.x() < xHigh)
                            && (k.z() >= zLow && k.z() < zHigh))
                    .collect(Collectors.toList());
            for (Vec3i key : tileEntityUpdateKeysToRemove) {
                this.tileEntityUpdates.remove(key);
            }
        }
    }

    public void updateBlock(ServerBlockChangePacket packet) {
        synchronized (this) {
            Position pos = packet.getRecord().getPosition();
            if (pos.getY() < 0 || pos.getY() >= 256) {
                CLIENT_LOG.error("Received out-of-bounds block update: %s", packet.getRecord());
                return;
            }
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final Vec3i changeKey = Vec3i.of(x, y, z);
            this.blockUpdates.put(changeKey, packet);
        }
    }

    public void updateTileEntity(final ServerUpdateTileEntityPacket packet) {
        synchronized (this) {
            Position pos = packet.getPosition();
            if (pos.getY() < 0 || pos.getY() >= 256) {
                CLIENT_LOG.error("Received out-of-bounds tile entity update: %s", pos);
                return;
            }
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final Vec3i changeKey = Vec3i.of(x, y, z);
            this.tileEntityUpdates.put(changeKey, packet);
        }
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cache.values().parallelStream()
                .map(ServerChunkDataPacket::new)
                .forEach(consumer);
        this.blockUpdates.values()
                .forEach(consumer);
        this.tileEntityUpdates.values()
                .forEach(consumer);
    }

    @Override
    public void reset(boolean full) {
        this.cache.clear();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d chunks", this.cache.size());
    }
}
