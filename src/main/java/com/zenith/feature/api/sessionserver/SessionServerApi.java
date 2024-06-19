package com.zenith.feature.api.sessionserver;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.sessionserver.model.*;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.jetbrains.annotations.Nullable;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.*;

public class SessionServerApi extends Api {
    public static SessionServerApi INSTANCE = new SessionServerApi();

    public SessionServerApi() {
        super("https://sessionserver.mojang.com");
    }

    public Optional<SessionProfileResponse> getProfile(final UUID uuid) {
        return get("/session/minecraft/profile/" + uuid.toString(), SessionProfileResponse.class);
    }

    /**
     * @throws RuntimeException on failure to join server
     */
    public void joinServer(final UUID uuid, final String accessToken, final String sharedSecret) {
        final JoinServerRequest request = new JoinServerRequest(uuid, accessToken, sharedSecret);
        final HttpRequest httpRequest = buildBaseRequest("/session/minecraft/join")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(request)))
            .header("Content-Type", "application/json")
            .build();
        try (var client = buildHttpClient()) {
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (!(response.statusCode() == 204 || response.statusCode() == 200)) {
                if (!response.body().isEmpty()) {
                    if (response.body().startsWith("<!DOCTYPE html>")) {
                        throw new RuntimeException("Client join has been rate limited by Mojang");
                    }
                    try {
                        var errorResponse = GSON.fromJson(response.body(), JoinServerErrorResponse.class);
                        throw new RuntimeException("Failed to join server: " + errorResponse.toString());
                    } catch (final Exception e) {
                        throw new RuntimeException("Failed to join server: " + response.body());
                    }
                }
                throw new RuntimeException("Failed to join server: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to join server", e);
        }
    }

    public Optional<GameProfile> hasJoined(final String name, final String serverId) {
        final HttpRequest httpRequest = buildBaseRequest("/session/minecraft/hasJoined?username=" + name + "&serverId=" + serverId)
            .GET()
            .build();
        try (var client = buildHttpClient()) {
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (response.body().startsWith("<!DOCTYPE html>")) {
                    DEFAULT_LOG.error("Player: {} join has been rate limited by Mojang", name);
                    return Optional.empty();
                }
                DEFAULT_LOG.error("Player: {} failed to join server. Status code: {}, Response body: {}", name, response.statusCode(), response.body());
                return Optional.empty();
            }
            return Optional.of(GSON.fromJson(response.body(), HasJoinedResponse.class).toGameProfile());
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to join server. name: {}, serverId: {}", name, serverId, e);
            return Optional.empty();
        }
    }

    public Optional<GameProfile> getProfileAndSkin(final UUID uuid) {
        final HttpRequest httpRequest = buildBaseRequest("/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false")
            .GET()
            .build();
        try (var client = buildHttpClient()) {
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                DEFAULT_LOG.error("Failed to get profile and skin for uuid: {}. Status code: {}, Response body: {}", uuid, response.statusCode(), response.body());
                return Optional.empty();
            }
            var profileAndSkin = Optional.of(GSON.fromJson(response.body(), MojangProfileAndSkin.class)).get();
            var propsResponse = profileAndSkin.properties();
            var props = new ArrayList<GameProfile.Property>(propsResponse.size());
            for (var prop : propsResponse) {
                props.add(new GameProfile.Property(prop.name(), prop.value(), prop.signature()));
            }
            var gameProfile = new GameProfile(profileAndSkin.uuid(), profileAndSkin.name());
            gameProfile.setProperties(props);
            return Optional.of(gameProfile);
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to get profile and skin for uuid: {}", uuid, e);
            return Optional.empty();
        }
    }

    public String getSharedSecret(String serverId, PublicKey publicKey, SecretKey secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(secretKey.getEncoded());
            digest.update(publicKey.getEncoded());
            return (new BigInteger(digest.digest())).toString(16);
        } catch (NoSuchAlgorithmException var5) {
            throw new IllegalStateException("Server ID hash algorithm unavailable.", var5);
        }
    }

    public @Nullable SecretKey generateClientKey() {
        SecretKey key;
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(128);
            key = gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            CLIENT_LOG.error("Failed to generate shared key.", e);
            return null;
        }
        return key;
    }
}
