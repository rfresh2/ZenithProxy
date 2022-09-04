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
