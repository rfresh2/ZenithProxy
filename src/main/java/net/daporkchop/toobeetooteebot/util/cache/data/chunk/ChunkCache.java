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

package net.daporkchop.toobeetooteebot.util.cache.data.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import net.daporkchop.lib.math.vector.i.Vec2i;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class ChunkCache implements CachedData, BiFunction<Column, Column, Column> {
    protected final Map<Vec2i, Column> cache = new ConcurrentHashMap<>();

    public void add(@NonNull Column column) {
        this.cache.merge(new Vec2i(column.getX(), column.getZ()), column, this);
        CACHE_LOG.debug("Cached chunk (%d, %d)", column.getX(), column.getZ());
    }

    /**
     * @deprecated do not call this directly!
     */
    @Override
    @Deprecated
    public Column apply(@NonNull Column existing, @NonNull Column add) {
        CACHE_LOG.trace("Chunk (%d, %d) is already cached, merging with existing", add.getX(), add.getZ());
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
        return this.cache.get(new Vec2i(x, z));
    }

    public void remove(int x, int z) {
        CACHE_LOG.debug("Server telling us to uncache chunk (%d, %d)", x, z);
        if (this.cache.remove(new Vec2i(x, z)) == null) {
            CACHE_LOG.warn("Could not remove column (%d, %d)! this is probably a server issue", x, z);
        }
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cache.values().stream()
                .map(ServerChunkDataPacket::new)
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
