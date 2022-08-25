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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class BlockChangeHandler implements HandlerRegistry.AsyncIncomingHandler<ServerBlockChangePacket, ClientSession> {
    static boolean handleChange(@NonNull BlockChangeRecord record) {
        try {
            CLIENT_LOG.debug("Handling block update: pos: [" + record.getPosition().getX() + ", " + record.getPosition().getY() + ", " + record.getPosition().getZ() + "], id: " + record.getBlock().getId() + ", data: " + record.getBlock().getData());
            CACHE.getChunkCache().updateBlock(new ServerBlockChangePacket(record));
        } catch (final Exception e) {
            CLIENT_LOG.error("Error applying block update", e);
        }
        return true;
    }

    @Override
    public boolean applyAsync(@NonNull ServerBlockChangePacket packet, @NonNull ClientSession session) {
        return handleChange(packet.getRecord());
    }

    @Override
    public Class<ServerBlockChangePacket> getPacketClass() {
        return ServerBlockChangePacket.class;
    }
}
