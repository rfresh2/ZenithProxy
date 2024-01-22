package com.zenith.feature.api;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import static com.zenith.Shared.*;

public abstract class Api {
    final String baseUrl;
    public Api(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    }

    protected <T> Optional<T> get(final String uri, final Class<T> clazz) {
        HttpRequest request = buildBaseRequest(uri)
            .GET()
            .build();
        try (HttpClient client = buildHttpClient()) {
            var response = client
                .send(request, HttpResponse.BodyHandlers.ofInputStream());
            return Optional.of(OBJECT_MAPPER.readValue(response.body(), clazz));
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to parse response", e);
            return Optional.empty();
        }
    }

    protected Optional<HttpResponse<String>> post(final String uri) {
        HttpRequest request = buildBaseRequest(uri)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        try (HttpClient client = buildHttpClient()) {
            var response = client
                .send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(response);
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to parse response", e);
            return Optional.empty();
        }
    }

    protected HttpRequest.Builder buildBaseRequest(final String uri) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + uri))
            .headers("User-Agent", "ZenithProxy/" + LAUNCH_CONFIG.version);
    }
}
