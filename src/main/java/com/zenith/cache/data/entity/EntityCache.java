package com.zenith.cache.data.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.feature.spectator.SpectatorSync;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.EVENT_BUS;

@Data
@Accessors(chain = true)
public class EntityCache implements CachedData {
    protected final Map<Integer, Entity> entities = new ConcurrentHashMap<>();
    protected final Map<UUID, EntityPlayer> players = new ConcurrentHashMap<>();
    protected final Cache<UUID, EntityPlayer> recentlyRemovedPlayers = CacheBuilder.newBuilder()
        // really we're looking for players in the last tick (with generous headroom for async scheduling)
        .expireAfterWrite(Duration.ofSeconds(2))
        .build();
    private static final double maxDistanceExpected = Math.pow(32, 2); // squared to speed up calc, no need to sqrt
    public static final int TICK_PRIORITY = -10000;

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer, final @NonNull TcpSession session) {
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
            this.players.clear();
        } else {
            // unload all entities
            // defer self-player reset logic to PlayerCache
            this.entities.keySet().removeIf(i -> i != CACHE.getPlayerCache().getEntityId());
            this.players.keySet().removeIf(uuid -> !uuid.equals(CACHE.getPlayerCache().getThePlayer().getUuid()));
        }
        this.recentlyRemovedPlayers.invalidateAll();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d entities", this.entities.size());
    }

    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            // must be ordered after ClientBotTick
            // ticks whether or not the bot is active
            // be careful with self player ticking, as we have split sources of truth between cache and bot
            of(ClientTickEvent.End.class, TICK_PRIORITY, this::tick)
        );
    }

    private void tick(ClientTickEvent.End event) {
        for (var entity : entities.values()) {
            entity.tick();
            for (var id : entity.getPassengerIds()) {
                var passenger = get(id);
                if (passenger == null) continue;
                tickPassenger(entity, passenger);
            }
        }
    }

    private void tickPassenger(Entity mount, Entity passenger) {
        // does not handle known edge cases where mojang overrides default attachment positioning code
        var vehicleEntityData = mount.getEntityData();
        var vehicleAttachment = vehicleEntityData.entityAttachment();
        var riderAttachment = passenger.getEntityData().entityAttachment();
        var vehicleAttachY = mount.getY() + (vehicleAttachment != null ? vehicleAttachment.passenger() : vehicleEntityData.height());
        var riderAttachY = riderAttachment != null ? riderAttachment.vehicle() : 0;
        var riderY = vehicleAttachY - riderAttachY;
        passenger
            .setX(mount.getX())
            .setY(riderY)
            .setZ(mount.getZ())
            .setBaseX(mount.getBaseX())
            .setBaseY(riderY)
            .setBaseZ(mount.getBaseZ());
        if (passenger instanceof EntityPlayer player && player.isSelfPlayer()) {
            SpectatorSync.syncPlayerPositionWithSpectators();
        }
        for (var pid : passenger.getPassengerIds()) {
            var passenger2 = get(pid);
            if (passenger2 == null) continue;
            tickPassenger(passenger, passenger2);
        }
    }

    public void add(@NonNull Entity entity) {
        this.entities.put(entity.getEntityId(), entity);
        if (entity instanceof EntityPlayer player && player.getUuid() != null) {
            this.players.put(player.getUuid(), player);
        }
    }

    public @Nullable Entity remove(int id)  {
        var entity = this.entities.remove(id);
        if (entity != null) entity.setRemoved(true);
        if (entity instanceof EntityPlayer player) {
            this.players.remove(player.getUuid());
            this.recentlyRemovedPlayers.put(player.getUuid(), player);
        }
        return entity;
    }

    public Optional<EntityPlayer> getRecentlyRemovedPlayer(UUID uuid) {
        return Optional.ofNullable(this.recentlyRemovedPlayers.getIfPresent(uuid));
    }

    public @Nullable Entity get(int id) {
        return this.entities.get(id);
    }

    public @Nullable Entity get(Entity entity) {
        return this.entities.get(entity.getEntityId());
    }

    public @Nullable Entity get(UUID uuid) {
        return this.players.get(uuid);
    }
}
