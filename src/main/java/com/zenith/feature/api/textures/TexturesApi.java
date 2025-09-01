package com.zenith.feature.api.textures;

import com.zenith.feature.api.Api;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Optional;

import static com.zenith.Globals.DEFAULT_LOG;

public class TexturesApi extends Api {
    public static final TexturesApi INSTANCE = new TexturesApi();

    public TexturesApi() {
        super("http://textures.minecraft.net/");
    }

    public Optional<byte[]> getTexture(final String url) {
        var httpRequest = buildBaseRequest("")
            .uri(URI.create(url))
            .GET()
            .build();
        try (var client = buildHttpClient()) {
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 && response.statusCode() != 304) {
                DEFAULT_LOG.error("Failed to get texture from url: {} - {}", url, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to get texture from url: {} : {}", url, e.getMessage());
            return Optional.empty();
        }
    }

}
