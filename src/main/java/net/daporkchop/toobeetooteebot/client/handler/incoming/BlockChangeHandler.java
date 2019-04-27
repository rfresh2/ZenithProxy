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

package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class BlockChangeHandler implements HandlerRegistry.IncomingHandler<ServerBlockChangePacket, PorkClientSession> {
    static void handleChange(@NonNull BlockChangeRecord record) {
        Position pos = record.getPosition();
        if (pos.getY() < 0 || pos.getY() >= 256) {
            CLIENT_LOG.trace("Received out-of-bounds block update: %s", record);
            return;
        }
        Column column = CACHE.getChunkCache().get(pos.getX() >> 4, pos.getZ() >> 4);
        if (column != null) {
            Chunk chunk = column.getChunks()[pos.getY() >> 4];
            if (chunk == null) {
                chunk = column.getChunks()[pos.getY() >> 4] = new Chunk(true);
            }
            chunk.getBlocks().set(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, record.getBlock());
        }
    }

    @Override
    public boolean apply(@NonNull ServerBlockChangePacket packet, @NonNull PorkClientSession session) {
        handleChange(packet.getRecord());
        return true;
    }

    @Override
    public Class<ServerBlockChangePacket> getPacketClass() {
        return ServerBlockChangePacket.class;
    }
}
