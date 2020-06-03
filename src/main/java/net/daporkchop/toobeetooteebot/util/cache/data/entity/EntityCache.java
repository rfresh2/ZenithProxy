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

package net.daporkchop.toobeetooteebot.util.cache.data.entity;

import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class EntityCache implements CachedData {
    protected final Map<Integer, Entity> cachedEntities = new ConcurrentHashMap<>(); //TODO: finish porklib primitive

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cachedEntities.values().forEach(entity -> entity.addPackets(consumer));
    }

    @Override
    public void reset(boolean full) {
        if (full) {
            this.cachedEntities.clear();
        } else {
            this.cachedEntities.keySet().removeIf(i -> i != CACHE.getPlayerCache().getEntityId());
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d entities", this.cachedEntities.size());
    }

    public void add(@NonNull Entity entity) {
        this.cachedEntities.put(entity.getEntityId(), entity);
    }

    public void remove(int id)  {
        this.cachedEntities.remove(id);
    }

    @SuppressWarnings("unchecked")
    public <E extends Entity> E get(int id)   {
        return (E) this.cachedEntities.get(id);
    }
}
