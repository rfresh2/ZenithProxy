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
import net.kyori.adventure.text.Component;

import java.security.PublicKey;
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
        entry.ping = in.getLatency();
        try {
            entry.textures.putAll(in.getProfile().getTextures());
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
        entry.properties.addAll(in.getProfile().getProperties());
        entry.sessionId = in.getSessionId();
        entry.expiresAt = in.getExpiresAt();
        entry.publicKey = in.getPublicKey();
        entry.keySignature = in.getKeySignature();
        return entry;
    }

    @NonNull
    protected final String name;

    @NonNull
    protected final UUID id;
    protected UUID sessionId;
    protected long expiresAt;
    protected PublicKey publicKey;
    protected byte[] keySignature;

    protected final Map<GameProfile.TextureType, GameProfile.Texture> textures = new EnumMap<>(GameProfile.TextureType.class);
    protected List<GameProfile.Property> properties = new ArrayList<>();

    protected Component displayName;

    protected GameMode gameMode;

    protected int ping;

    public PlayerListEntry toMCProtocolLibEntry()   {
        PlayerListEntry entry = new PlayerListEntry(
            this.id,
            new GameProfile(this.id, this.name),
            true,
            this.ping,
            this.gameMode,
            this.displayName,
            this.sessionId,
            this.expiresAt,
            this.publicKey,
            this.keySignature
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
