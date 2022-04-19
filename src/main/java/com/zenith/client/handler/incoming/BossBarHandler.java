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

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import lombok.NonNull;
import com.zenith.client.PorkClientSession;
import com.zenith.util.handler.HandlerRegistry;

import java.util.function.Consumer;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

/**
 * @author DaPorkchop_
 */
public class BossBarHandler implements HandlerRegistry.AsyncIncomingHandler<ServerBossBarPacket, PorkClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerBossBarPacket packet, @NonNull PorkClientSession session) {
        Consumer<ServerBossBarPacket> consumer = pck -> {
            throw new IllegalStateException();
        };
        switch (packet.getAction())    {
            case ADD:
                consumer = CACHE.getBossBarCache()::add;
                break;
            case REMOVE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = CACHE.getBossBarCache()::remove;
                break;
            case UPDATE_HEALTH:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setHealth(pck.getHealth());
                break;
            case UPDATE_TITLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setTitle(pck.getTitle());
                break;
            case UPDATE_STYLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setColor(pck.getColor()).setDivision(pck.getDivision());
                break;
            case UPDATE_FLAGS:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setDarkenSky(pck.getDarkenSky()).setDragonBar(pck.isDragonBar());
                break;
        }
        consumer.accept(packet);
        return true;
    }

    @Override
    public Class<ServerBossBarPacket> getPacketClass() {
        return ServerBossBarPacket.class;
    }
}
