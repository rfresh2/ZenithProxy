package com.zenith.feature.api;

import com.google.common.base.Suppliers;
import io.netty.resolver.DefaultAddressResolverGroup;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

public abstract class Api {
    final String baseUrl;
    final Supplier<HttpClient> httpClientSupplier = Suppliers.memoize(this::buildHttpClient);

    public Api(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected HttpClient buildHttpClient() {
        return HttpClient.create()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            .secure()
            .headers(h -> h.add("User-Agent", "ZenithProxy/" + LAUNCH_CONFIG.version))
            .followRedirect(true)
            .baseUrl(baseUrl);
    }

    protected <T> Optional<T> get(final String uri, final Class<T> clazz) {
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
