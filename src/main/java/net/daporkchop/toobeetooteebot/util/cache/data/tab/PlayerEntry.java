/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.message.Message;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class PlayerEntry {
    public static PlayerEntry fromMCProtocolLibEntry(@NonNull PlayerListEntry in)  {
        PlayerEntry entry = new PlayerEntry(in.getProfile().getName(), in.getProfile().getId());
        entry.displayName = in.getDisplayName();
        entry.gameMode = in.getGameMode();
        entry.ping = in.getPing();
        entry.textures.putAll(in.getProfile().getTextures());
        entry.properties.addAll(in.getProfile().getProperties());
        return entry;
    }

    @NonNull
    protected final String name;

    @NonNull
    protected final UUID id;

    protected final Map<GameProfile.TextureType, GameProfile.Texture> textures = new EnumMap<>(GameProfile.TextureType.class);
    protected List<GameProfile.Property> properties = new ArrayList<>();

    protected Message displayName;

    protected GameMode gameMode;

    protected int ping;

    public PlayerListEntry toMCProtocolLibEntry()   {
        PlayerListEntry entry = new PlayerListEntry(
                new GameProfile(this.id, this.name),
                this.gameMode,
                this.ping,
                this.displayName
        );
        entry.getProfile().getTextures().putAll(this.textures);
        entry.getProfile().getProperties().addAll(this.properties);
        return entry;
    }
}
