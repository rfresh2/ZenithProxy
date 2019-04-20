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

package net.daporkchop.toobeetooteebot.util.cache;

import lombok.Getter;
import net.daporkchop.toobeetooteebot.util.Constants;
import net.daporkchop.toobeetooteebot.util.cache.data.ChunkCache;
import net.daporkchop.toobeetooteebot.util.cache.data.PlayerCache;
import net.daporkchop.toobeetooteebot.util.cache.data.ServerProfileCache;
import net.daporkchop.toobeetooteebot.util.cache.data.bossbar.BossBarCache;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.EntityCache;
import net.daporkchop.toobeetooteebot.util.cache.data.tab.TabListCache;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author DaPorkchop_
 */
@Getter
public class DataCache implements Constants {
    private static final Collection<Field> dataFields = new ArrayDeque<>();

    static {
        try {
            for (Field field : DataCache.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (CachedData.class.isAssignableFrom(field.getType())) {
                    if (CONFIG.getBoolean("debug.printDataFields")) {
                        logger.debug("Found data field: ${0}", field.getName());
                    }
                    dataFields.add(field);
                } else if (CONFIG.getBoolean("debug.printDataFields")) {
                    logger.debug("Class ${0} is not a valid data field.", field.getType().getCanonicalName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (CONFIG.getBoolean("debug.printDataFields")) {
            logger.debug("Found a total of ${0} data fields.", dataFields.size());
        }
    }

    private final ThreadLocal<Collection<CachedData>> dataCache = ThreadLocal.withInitial(() -> new ArrayList<>(dataFields.size()));

    private final ChunkCache chunkCache = new ChunkCache();
    private final TabListCache tabListCache = new TabListCache();
    private final BossBarCache bossBarCache = new BossBarCache();
    private final EntityCache entityCache = new EntityCache();
    private final PlayerCache playerCache = new PlayerCache(); //TODO: remove and cache player as entity
    private final ServerProfileCache profileCache = new ServerProfileCache();

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
        logger.debug("Clearing cache...");

        try {
            this.getAllData().forEach(d -> d.reset(full));

            logger.debug("Cache cleared.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear cache", e);
        }
        return true;
    }
}
