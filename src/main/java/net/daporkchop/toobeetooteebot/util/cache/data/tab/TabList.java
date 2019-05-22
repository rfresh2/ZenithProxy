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

package net.daporkchop.toobeetooteebot.util.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.message.Message;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.toobeetooteebot.util.Constants;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
@Accessors(chain = true)
public class TabList implements Constants {
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

    public void remove(@NonNull PlayerListEntry entry) {
        PlayerEntry removed = this.entries.remove(entry.getProfile().getId());
        if (removed == null && CONFIG.getBoolean("debug.server.cache.printunknownplayers")) {
            CACHE_LOG.error("Could not remove player with UUID: %s", entry.getProfile().getId());
        } else if (removed != null) {
            CACHE_LOG.debug("Removed %s (%s) from tab list", removed.name, removed.id);
            WEBSOCKET_SERVER.removePlayer(removed.id);
        }
    }

    public PlayerEntry get(@NonNull PlayerListEntry entry) {
        PlayerEntry e = this.entries.get(entry.getProfile().getId());
        if (e == null) {
            if (CONFIG.getBoolean("debug.server.cache.unknownplayers")) {
                CACHE_LOG.error("Could not find player with UUID: %s", entry.getProfile().getId());
            }
            return new PlayerEntry("", entry.getProfile().getId());
        }
        return e;
    }

    public Collection<PlayerEntry> getEntries() {
        return this.entries.values();
    }
}
