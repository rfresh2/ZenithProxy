package com.zenith.cache.data.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;

@Data
public class EntityCache implements CachedData {
    protected final Map<Integer, Entity> entities = new ConcurrentHashMap<>();
    protected final Cache<UUID, EntityPlayer> recentlyRemovedPlayers = CacheBuilder.newBuilder()
        // really we're looking for players in the last tick (with generous headroom for async scheduling)
        .expireAfterWrite(Duration.ofSeconds(2))
        .build();
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        // it would be preferable to not have this intermediary list but we need to sort :/
        // size is a rough estimate, some entities will provide much more packets than others
        final List<Packet> packets = new ArrayList<>(this.entities.size() * 6);
        this.entities.values().forEach(entity -> entity.addPackets(packets::add));
        // send all ClientboundAddEntityPackets first
        // some entity metadata references other entities that need to exist first
        for (int i = 0; i < packets.size(); i++) {
            var packet = packets.get(i);
            if (packet instanceof ClientboundAddEntityPacket) {
                consumer.accept(packet);
            }
        }
        for (int i = 0; i < packets.size(); i++) {
            var packet = packets.get(i);
            if (!(packet instanceof ClientboundAddEntityPacket)) {
                consumer.accept(packet);
            }
        }
    }


    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.PROTOCOL_SWITCH) {
            this.entities.clear();
        } else {
            // unload all entities
            // defer self-player reset logic to PlayerCache
            this.entities.keySet().removeIf(i -> i != CACHE.getPlayerCache().getEntityId());
        }
        this.recentlyRemovedPlayers.invalidateAll();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d entities", this.entities.size());
    }

    public void add(@NonNull Entity entity) {
        this.entities.put(entity.getEntityId(), entity);
    }

    public Entity remove(int id)  {
        Entity entity = this.entities.remove(id);
        if (entity instanceof EntityPlayer player)
            this.recentlyRemovedPlayers.put(player.getUuid(), player);
        return entity;
    }

    public Optional<EntityPlayer> getRecentlyRemovedPlayer(UUID uuid) {
        return Optional.ofNullable(this.recentlyRemovedPlayers.getIfPresent(uuid));
    }

    public Entity get(int id) {
        return this.entities.get(id);
    }

    // todo: this is not particularly efficient but is currently used infrequently.
    //  if there are higher frequency use cases, consider building a secondary cached map of uuids to entity
    public Entity get(UUID uuid) {
        return this.entities.values().stream()
            .filter(entity -> entity.getUuid().equals(uuid))
            .findFirst()
            .orElse(null);
    }
}
