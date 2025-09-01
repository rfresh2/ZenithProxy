package com.zenith.feature.skin;

import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.minotar.MinotarApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.feature.api.textures.TexturesApi;
import org.geysermc.mcprotocollib.auth.GameProfile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Globals.CONFIG;

public class SkinRetriever {
    private SkinRetriever() {}

    /**
     * 64x64 png player head image
     */
    public static Optional<byte[]> getRenderedAvatar(GameProfile profile) {
        try {
            if (profile != null && profile.getId() != null) {
                Optional<byte[]> renderedTexture = getSkinData(profile);
                if (renderedTexture.isPresent()) {
                    return renderedTexture;
                } else { // failed asking mojang for skin data, fallback to avatar api's
                    final UUID uuid = profile.getId();
                    var bytes = MinotarApi.INSTANCE.getAvatar(uuid)
                        .or(() -> CraftheadApi.INSTANCE.getAvatar(uuid))
                        .orElseThrow(() -> new IOException("Unable to download server icon for \"" + uuid + "\""));
                    return Optional.of(bytes);
                }
            } else { // we don't know the uuid yet, fallback to username based lookup
                final String username = CONFIG.authentication.username;
                if (username.equals("Unknown") || username.isEmpty()) {
                    return Optional.empty();
                }
                var bytes = MinotarApi.INSTANCE.getAvatar(username)
                    .or(() -> CraftheadApi.INSTANCE.getAvatar(username))
                    .orElseThrow(() -> new IOException("Unable to download server icon for \"" + username + "\""));
                return Optional.of(bytes);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 64x64 png of raw player skin data
     */
    private static Optional<byte[]> getSkinData(GameProfile profile) {
        var skinTexture = profile.getTexture(GameProfile.TextureType.SKIN, false);

        if (skinTexture == null || skinTexture.getURL() == null) {
            // try to lookup skin data from mojang servers
            var gameProfileOptional = SessionServerApi.INSTANCE.getProfileAndSkin(profile.getId());
            if (gameProfileOptional.isPresent()) {
                var gameProfile = gameProfileOptional.get();
                skinTexture = gameProfile.getTexture(GameProfile.TextureType.SKIN, false);
            }
        }

        if (skinTexture != null && skinTexture.getURL() != null) {
            var textureOptional = TexturesApi.INSTANCE.getTexture(skinTexture.getURL());
            if (textureOptional.isPresent()) {
                var skinData = textureOptional.get();
                return Optional.of(SkinRenderer.renderHead(skinData, 8));
            }
        }
        return Optional.empty();
    }

}
