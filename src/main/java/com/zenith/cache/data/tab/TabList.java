package com.zenith.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.zenith.Shared.CACHE_LOG;
import static com.zenith.Shared.CONFIG;


@Getter
@Setter
@Accessors(chain = true)
public class TabList {
    protected final Map<UUID, PlayerListEntry> entries = new ConcurrentHashMap<>();
    @NonNull
    protected Component header = Component.text("");
    @NonNull
    protected Component footer = Component.text("");
    protected long lastUpdate = 0L;

    public void add(@NonNull PlayerListEntry entry) {
        this.entries.put(entry.getProfile().getId(), entry);
    }

    public Optional<PlayerListEntry> remove(@NonNull PlayerListEntry entry) {
        PlayerListEntry removed = this.entries.remove(entry.getProfile().getId());
        if (removed == null && CONFIG.debug.server.cache.unknownplayers) {
            CACHE_LOG.error("Could not remove player with UUID: {}", entry.getProfile().getId());
        }
        return Optional.ofNullable(removed);
    }

    public Optional<PlayerListEntry> remove(@NonNull UUID uuid) {
        PlayerListEntry removed = this.entries.remove(uuid);
        if (removed == null && CONFIG.debug.server.cache.unknownplayers) {
            CACHE_LOG.error("Could not remove player with UUID: {}", uuid);
        }
        return Optional.ofNullable(removed);
    }

    public Optional<PlayerListEntry> get(UUID uuid) {
        return Optional.ofNullable(this.entries.get(uuid));
    }

    public Optional<PlayerListEntry> getFromName(final String username) {
        return this.entries.values().stream().filter(v -> v.getName().equals(username)).findFirst();
    }

    public Collection<PlayerListEntry> getEntries() {
        return this.entries.values();
    }
}
