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

package com.zenith.util.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
@Accessors(chain = true)
public class TabList {
    protected final Map<UUID, PlayerEntry> entries = new ConcurrentHashMap<>();
    @NonNull
    protected String header = "{\"text\":\"\"}";
    @NonNull
    protected String footer = "{\"text\":\"\"}";

    public void add(@NonNull PlayerListEntry entry) {
        CACHE_LOG.debug("Added %s (%s) to tab list", entry.getProfile().getName(), entry.getProfile().getId());

        PlayerEntry coolEntry = PlayerEntry.fromMCProtocolLibEntry(entry);
        this.entries.put(entry.getProfile().getId(), coolEntry);
        WEBSOCKET_SERVER.updatePlayer(coolEntry);
    }

    public Optional<PlayerEntry> remove(@NonNull PlayerListEntry entry) {
        PlayerEntry removed = this.entries.remove(entry.getProfile().getId());
        if (removed == null && CONFIG.debug.server.cache.unknownplayers) {
            CACHE_LOG.error("Could not remove player with UUID: %s", entry.getProfile().getId());
        } else if (removed != null) {
            CACHE_LOG.debug("Removed %s (%s) from tab list", removed.name, removed.id);
            WEBSOCKET_SERVER.removePlayer(removed.id);
        }
        return Optional.ofNullable(removed);
    }

    public PlayerEntry get(@NonNull PlayerListEntry entry) {
        PlayerEntry e = this.entries.get(entry.getProfile().getId());
        if (e == null) {
            if (CONFIG.debug.server.cache.unknownplayers) {
                CACHE_LOG.error("Could not find player with UUID: %s", entry.getProfile().getId());
            }
            return new PlayerEntry("", entry.getProfile().getId());
        }
        return e;
    }

    public Optional<PlayerEntry> get(UUID uuid) {
        return Optional.ofNullable(this.entries.get(uuid));
    }

    public Collection<PlayerEntry> getEntries() {
        return this.entries.values();
    }
}
