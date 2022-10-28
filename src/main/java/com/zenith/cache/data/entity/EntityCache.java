package com.zenith.cache.data.entity;

import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.util.Constants.CACHE;

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
            try {
                ((EntityPlayer) this.cachedEntities.get(CACHE.getPlayerCache().getEntityId())).health = 20.0f;
            } catch (final Throwable e) {
                // do nothing
            }
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
