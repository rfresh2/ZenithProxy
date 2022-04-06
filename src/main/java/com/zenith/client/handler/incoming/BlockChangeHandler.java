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

package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import lombok.NonNull;
import com.zenith.client.PorkClientSession;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class BlockChangeHandler implements HandlerRegistry.IncomingHandler<ServerBlockChangePacket, PorkClientSession> {
    static void handleChange(@NonNull BlockChangeRecord record) {
        Position pos = record.getPosition();
        if (pos.getY() < 0 || pos.getY() >= 256) {
            CLIENT_LOG.error("Received out-of-bounds block update: %s", record);
            return;
        }
        Column column = CACHE.getChunkCache().get(pos.getX() >> 4, pos.getZ() >> 4);
        if (column != null) {
            Chunk chunk = column.getChunks()[pos.getY() >> 4];
            if (chunk == null) {
                chunk = column.getChunks()[pos.getY() >> 4] = new Chunk(column.hasSkylight());
            } else {
                SERVER_LOG.warn("No Chunk found for block update with position: " + pos);
            }
            chunk.getBlocks().set(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, record.getBlock());
            SERVER_LOG.debug("Updating block: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] Id: " + record.getBlock().getId() + ", data: " + record.getBlock().getData());
        } else {
            SERVER_LOG.error("Received block update for uncached chunk with position: " + pos);
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
