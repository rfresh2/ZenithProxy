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

package com.zenith.cache;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.ServerProfileCache;
import com.zenith.cache.data.bossbar.BossBarCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.cache.data.stats.StatisticsCache;
import com.zenith.cache.data.tab.TabListCache;
import com.zenith.server.ServerConnection;
import com.zenith.util.Wait;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DataCache {
    protected static final Collection<Field> dataFields = new ArrayDeque<>();

    static {
        try {
            for (Field field : DataCache.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (CachedData.class.isAssignableFrom(field.getType())) {
                    if (CONFIG.debug.printDataFields) {
                        CACHE_LOG.debug("Found data field: %s", field.getName());
                    }
                    dataFields.add(field);
                } else if (CONFIG.debug.printDataFields) {
                    CACHE_LOG.debug("Class %s is not a valid data field.", field.getType().getCanonicalName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (CONFIG.debug.printDataFields) {
            CACHE_LOG.debug("Found a total of %d data fields.", dataFields.size());
        }
    }
    protected final ChunkCache chunkCache = new ChunkCache();
    protected final TabListCache tabListCache = new TabListCache();
    protected final BossBarCache bossBarCache = new BossBarCache();
    protected final EntityCache entityCache = new EntityCache();
    protected final PlayerCache playerCache = new PlayerCache(entityCache);
    protected final ServerProfileCache profileCache = new ServerProfileCache();
    protected final StatisticsCache statsCache = new StatisticsCache();

    public Collection<CachedData> getAllData() {
        return Arrays.asList(profileCache, chunkCache, statsCache, tabListCache, bossBarCache,  entityCache, playerCache);
    }

    // get a limited selection of cache data
    // mainly we don't want to not send the proxy client's player cache
    public Collection<CachedData> getAllDataSpectator(final PlayerCache spectatorPlayerCache) {
        return Arrays.asList(chunkCache, tabListCache, bossBarCache, entityCache, spectatorPlayerCache);
    }

    public boolean reset(boolean full) {
        CACHE_LOG.debug("Clearing " + (full ? "full" : "partial") +" cache...");

        try {
            this.getAllData().forEach(d -> d.reset(full));

            CACHE_LOG.debug("Cache cleared.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear cache", e);
        }
        return true;
    }

    public static void sendCacheData(final Collection<CachedData> cacheData, final ServerConnection connection) {
        cacheData.forEach(data -> {
            if (CONFIG.debug.server.cache.sendingmessages) {
                String msg = data.getSendingMessage();
                if (msg == null)    {
                    SERVER_LOG.debug("Sending data to client %s", data.getClass().getCanonicalName());
                } else {
                    SERVER_LOG.debug(msg);
                }
            }
            data.getPackets(p -> {
                if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                    return;
                }
                connection.send(p);
            });
            ForkJoinPool.commonPool().submit(() -> {
                // client needs to receive chunks first.
                // this wait is kinda arbitrary and may be too short or long for some clients
                // likely dependent on client net speed
                // we don't have a good hook into when the client is done receiving chunks though.
                // waiting too long will appear as though chunks are visibly updating for client during play
                Wait.waitALittle(1);
                data.getPackets(p -> {
                    if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                        connection.send(p);
                    }
                });
            });
        });
    }
}
