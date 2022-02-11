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

package com.zenith.util.cache;

import lombok.Getter;
import com.zenith.util.cache.data.chunk.ChunkCache;
import com.zenith.util.cache.data.PlayerCache;
import com.zenith.util.cache.data.ServerProfileCache;
import com.zenith.util.cache.data.bossbar.BossBarCache;
import com.zenith.util.cache.data.entity.EntityCache;
import com.zenith.util.cache.data.stats.StatisticsCache;
import com.zenith.util.cache.data.tab.TabListCache;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;

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

    protected final ThreadLocal<Collection<CachedData>> dataCache = ThreadLocal.withInitial(() -> new ArrayList<>(dataFields.size()));

    protected final ChunkCache chunkCache = new ChunkCache();
    protected final TabListCache tabListCache = new TabListCache();
    protected final BossBarCache bossBarCache = new BossBarCache();
    protected final EntityCache entityCache = new EntityCache();
    protected final PlayerCache playerCache = new PlayerCache();
    protected final ServerProfileCache profileCache = new ServerProfileCache();
    protected final StatisticsCache statsCache = new StatisticsCache();

    public Collection<CachedData> getAllData() {
        Collection<CachedData> collection = this.dataCache.get();
        collection.clear();
        dataFields.forEach(field -> {
            try {
                collection.add((CachedData) field.get(this));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return collection;
    }

    public boolean reset(boolean full) {
        CACHE_LOG.debug("Clearing cache...");

        try {
            this.getAllData().forEach(d -> d.reset(full));

            CACHE_LOG.debug("Cache cleared.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear cache", e);
        }
        return true;
    }
}
