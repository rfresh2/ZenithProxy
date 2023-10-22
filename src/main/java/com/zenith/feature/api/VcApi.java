package com.zenith.feature.api;

import com.google.common.base.Suppliers;
import com.zenith.feature.api.model.PlaytimeResponse;
import com.zenith.feature.api.model.SeenResponse;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

public class VcApi {
    private final Supplier<HttpClient> httpClientSupplier = Suppliers.memoize(() -> HttpClient.create()
        .secure()
        .headers(h -> h.add("User-Agent", "ZenithProxy/" + LAUNCH_CONFIG.version))
        .followRedirect(true)
        .baseUrl("https://api.2b2t.vc"));

    public Optional<SeenResponse> getLastSeen(final String playerName) {
        return get("/seen?playerName=" + playerName, SeenResponse.class);
    }

    public Optional<SeenResponse> getFirstSeen(final String playerName) {
        return get("/firstSeen?playerName=" + playerName, SeenResponse.class);
    }

    public Optional<PlaytimeResponse> getPlaytime(final String playerName) {
        return get("/playtime?playerName=" + playerName, PlaytimeResponse.class);
    }

    private <T> Optional<T> get(final String uri, final Class<T> clazz) {
        try {
            InputStream data = httpClientSupplier.get()
                .get()
                .uri(uri)
                .responseContent()
                .aggregate()
                .asInputStream()
                .block();
            return Optional.of(OBJECT_MAPPER.readValue(data, clazz));
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to parse response", e);
            return Optional.empty();
        }
    }

}
