package com.zenith.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

import static com.zenith.Shared.CACHE_LOG;
import static com.zenith.Shared.CONFIG;


@Getter
@Setter
@Accessors(chain = true)
public class TabList {
    protected final Map<UUID, PlayerEntry> entries = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());
    @NonNull
    protected String header = "{\"text\":\"\"}";
    @NonNull
    protected String footer = "{\"text\":\"\"}";

    public void add(@NonNull PlayerListEntry entry) {
        PlayerEntry coolEntry = PlayerEntry.fromMCProtocolLibEntry(entry);
        this.entries.put(entry.getProfile().getId(), coolEntry);
    }

    public Optional<PlayerEntry> remove(@NonNull PlayerListEntry entry) {
        PlayerEntry removed = this.entries.remove(entry.getProfile().getId());
        if (removed == null && CONFIG.debug.server.cache.unknownplayers) {
            CACHE_LOG.error("Could not remove player with UUID: {}", entry.getProfile().getId());
        }
        return Optional.ofNullable(removed);
    }

    public PlayerEntry get(@NonNull PlayerListEntry entry) {
        PlayerEntry e = this.entries.get(entry.getProfile().getId());
        if (e == null) {
            if (CONFIG.debug.server.cache.unknownplayers) {
                CACHE_LOG.error("Could not find player with UUID: {}", entry.getProfile().getId());
            }
            return new PlayerEntry("", entry.getProfile().getId());
        }
        return e;
    }

    public Optional<PlayerEntry> get(UUID uuid) {
        return Optional.ofNullable(this.entries.get(uuid));
    }

    public Optional<PlayerEntry> getFromName(final String username) {
        return this.entries.values().stream().filter(v -> v.name.equals(username)).findFirst();
    }

    public Collection<PlayerEntry> getEntries() {
        return this.entries.values();
    }
}
