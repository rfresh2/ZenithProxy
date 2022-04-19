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

package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.zenith.util.Wait;
import lombok.NonNull;
import com.zenith.client.PorkClientSession;
import com.zenith.util.cache.data.entity.Entity;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

/**
 * @author DaPorkchop_
 */
public class EntityPropertiesHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityPropertiesPacket, PorkClientSession> {
    @Override
    public void applyAsync(@NonNull ServerEntityPropertiesPacket packet, @NonNull PorkClientSession session) {
        int iterCount = 0;
        while (!updateEntity(packet)) {
            Wait.waitALittleMs(50);
            iterCount++;
            if (iterCount > 3) {
                CLIENT_LOG.warn("Received ServerEntityPropertiesPacket for invalid entity (id=%d)", packet.getEntityId());
                break;
            }
        }
    }

    private boolean updateEntity(ServerEntityPropertiesPacket packet) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.setProperties(packet.getAttributes());
        return true;
    }

    @Override
    public Class<ServerEntityPropertiesPacket> getPacketClass() {
        return ServerEntityPropertiesPacket.class;
    }
}
