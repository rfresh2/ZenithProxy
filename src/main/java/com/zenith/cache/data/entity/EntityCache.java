package com.zenith.cache.data.entity;

import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;

public class EntityCache implements CachedData {
    protected final ConcurrentHashMap<Integer, Entity> cachedEntities = new ConcurrentHashMap<>();
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cachedEntities.values().forEach(entity -> entity.addPackets(consumer));
//        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
//            this::reapDeadEntities,
//            5L,
//            5L,
//            TimeUnit.MINUTES);
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

    public Entity remove(int id)  {
        return this.cachedEntities.remove(id);
    }

    @SuppressWarnings("unchecked")
    public <E extends Entity> E get(int id) {
        Entity entity = this.cachedEntities.get(id);
        if (entity == null) return null;
        return (E) entity;
    }

    // todo: this is not particularly efficient but is currently used infrequently.
    //  if there are higher frequency use cases, consider building a secondary cached map of uuids to entity
    public <E extends Entity> E get(UUID uuid) {
        return this.cachedEntities.values().stream()
            .filter(entity -> entity.getUuid().equals(uuid))
            .map(entity -> (E) entity)
            .findFirst()
            .orElse(null);
    }

    public Map<Integer, Entity> getEntities() { return this.cachedEntities;}

    private void reapDeadEntities() {
        if (!Proxy.getInstance().isConnected()) return;
        int playerChunkX = (int) CACHE.getPlayerCache().getX() >> 4;
        int playerChunkZ = ((int) CACHE.getPlayerCache().getZ()) >> 4;
        this.cachedEntities.values()
            .removeIf(entity -> distanceOutOfRange(
                playerChunkX,
                playerChunkZ,
                ((int) entity.getX()) >> 4,
                ((int) entity.getZ()) >> 4));
    }

    private boolean distanceOutOfRange(final int x1, final int y1, final int x2, final int y2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) > maxDistanceExpected;
    }
}
