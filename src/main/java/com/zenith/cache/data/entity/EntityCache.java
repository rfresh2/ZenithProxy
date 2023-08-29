package com.zenith.cache.data.entity;

import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;

public class EntityCache implements CachedData {
    protected final Map<Integer, Entity> cachedEntities = Collections.synchronizedMap(new Int2ObjectOpenHashMap<>());


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

    public Map<Integer, Entity> getEntities() { return this.cachedEntities;}
}
