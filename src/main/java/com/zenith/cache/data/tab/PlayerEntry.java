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

package com.zenith.cache.data.tab;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

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
        try {
            entry.textures.putAll(in.getProfile().getTextures());
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
        entry.properties.addAll(in.getProfile().getProperties());
        return entry;
    }

    @NonNull
    protected final String name;

    @NonNull
    protected final UUID id;

    protected final Map<GameProfile.TextureType, GameProfile.Texture> textures = new EnumMap<>(GameProfile.TextureType.class);
    protected List<GameProfile.Property> properties = new ArrayList<>();

    protected String displayName;

    protected GameMode gameMode;

    protected int ping;

    public PlayerListEntry toMCProtocolLibEntry()   {
        PlayerListEntry entry = new PlayerListEntry(
                new GameProfile(this.id, this.name),
                this.gameMode,
                this.ping,
                this.displayName,
                false
        );
        try {
            entry.getProfile().getTextures().putAll(this.textures);
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
        entry.getProfile().getProperties().addAll(this.properties);
        return entry;
    }
}
