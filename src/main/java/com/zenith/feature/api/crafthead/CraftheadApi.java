package com.zenith.feature.api.crafthead;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.crafthead.model.CraftheadProfileResponse;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.DEFAULT_LOG;

public class CraftheadApi extends Api {
    public CraftheadApi() {
        super("https://crafthead.net");
    }

    public Optional<CraftheadProfileResponse> getProfile(final String username) {
        return get("/profile/" + username, CraftheadProfileResponse.class);
    }

    public Optional<CraftheadProfileResponse> getProfile(final UUID uuid) {
        return get("/profile/" + uuid.toString(), CraftheadProfileResponse.class);
    }

    public Optional<byte[]> getAvatar(final String username) {
        HttpRequest request = buildBaseRequest("/helm/" + username + "/64")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        try (var client = buildHttpClient()) {
            var response = client
                .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                DEFAULT_LOG.error("Got status code {} from Crafthead for username avatar: {}", response.statusCode(), username);
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to get avatar from Crafthead for username: {}", username, e);
            return Optional.empty();
        }
    }

    public Optional<byte[]> getAvatar(final UUID uuid) {
        HttpRequest request = buildBaseRequest("/helm/" + uuid.toString() + "/64")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        try (var client = buildHttpClient()) {
            var response = client
                .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                DEFAULT_LOG.error("Got status code {} from Crafthead for UUID avatar: {}", response.statusCode(), uuid);
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to get avatar from Crafthead for username: {}", uuid, e);
            return Optional.empty();
        }
    }
}
