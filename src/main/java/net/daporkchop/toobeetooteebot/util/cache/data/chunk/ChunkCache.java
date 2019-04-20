/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.util.cache.data.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.UpdatedTileType;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import net.daporkchop.lib.math.vector.i.Vec2i;
import net.daporkchop.lib.math.vector.i.Vec3i;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
public class ChunkCache implements CachedData {
    private final Map<Vec2i, Column> cache = new ConcurrentHashMap<>();

    public void add(@NonNull Column column) {
        if (this.cache.put(new Vec2i(column.getX(), column.getZ()), column) != null)    {
            throw new IllegalStateException(String.format("Chunk (%d,%d) is already cached! (this is probably a server issue)", column.getX(), column.getZ()));
        }
    }

    public Column get(int x, int z)   {
        return this.cache.get(new Vec2i(x, z));
    }

    public void remove(int x, int z)    {
        if (this.cache.remove(new Vec2i(x, z)) == null) {
            throw new IllegalStateException(String.format("Could not remove column (%d,%d)! this is probably a server issue", x, z));
        }
    }

    @Override
    public void getPacketsSimple(@NonNull Consumer<Packet> consumer) {
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
