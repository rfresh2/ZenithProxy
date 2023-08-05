package com.zenith.feature.autoupdater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.Proxy;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Objects;

import static com.zenith.Shared.DEFAULT_LOG;
import static com.zenith.Shared.LAUNCH_CONFIG;

public class RestAutoUpdater extends AutoUpdater {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestAutoUpdater() {
        this.httpClient = HttpClient.create()
            .secure()
            // todo: move repo and user to config
            .baseUrl("https://api.github.com/repos/rfresh2/ZenithProxy")
            // todo: remove auth token
            .headers(h -> h.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + System.getenv("GITHUB_TOKEN")))
            .headers(h -> h.add(HttpHeaderNames.ACCEPT, "application/vnd.github+json"))
            .headers(h -> h.add("X-GitHub-Api-Version", "2022-11-28"));
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() {
        if (!validReleaseChannel(LAUNCH_CONFIG.release_channel)) {
            DEFAULT_LOG.error("Invalid release channel: {}", LAUNCH_CONFIG.release_channel);
            return;
        }
        super.start();
    }

    public boolean validReleaseChannel(final String in) {
        return List.of("git", "java", "linux-native", "prerelease-linux-native").contains(in);
    }

    @Override
    public void updateCheck() {
        httpClient
            .get()
            .uri("/releases?per_page=100")
            .responseContent()
            .aggregate()
            .asString()
            .flatMap(response -> {
                String releaseId = parseLatestReleaseId(response);
                if (releaseId == null) {
                    return Mono.empty();
                }
                return getVersionFromRelease(releaseId)
                    .map(versionFromRelease -> {
                        if (versionLooksCorrect(versionFromRelease)) {
                            if (!Objects.equals(Proxy.getVersion(), versionFromRelease)) {
                                if (!getUpdateAvailable()) DEFAULT_LOG.info(
                                    "New update on release channel {}! Current version: {} New Version: {}!",
                                    LAUNCH_CONFIG.release_channel,
                                    Proxy.getVersion(),
                                    versionFromRelease);
                                setUpdateAvailable(true);
                            }
                        } else DEFAULT_LOG.warn("Failed to parse version from release: '{}'", versionFromRelease);
                        return Mono.empty();
                    });
            })
            .block();
    }

    private boolean versionLooksCorrect(final String version) {
        return version != null && version.length() == 8 && version.matches("[0-9a-f]+");
    }

    private String parseLatestReleaseId(String response) {
        try {
            JsonNode releases = objectMapper.readTree(response);

            List<JsonNode> releaseNodes = releases.findParents("tag_name");
            releaseNodes.removeIf(node -> !node.get("tag_name").textValue().matches(LAUNCH_CONFIG.release_channel + "-.*"));

            releaseNodes.sort((a, b) -> b.get("published_at").asText().compareTo(a.get("published_at").asText()));

            if (!releaseNodes.isEmpty()) {
                return releaseNodes.get(0).get("id").asText();
            }
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to parse latest release ID.", e);
        }

        return null;
    }

    private Mono<String> getVersionFromRelease(final String releaseId) {
        return httpClient
            .get()
            .uri("/releases/" + releaseId)
            .responseContent()
            .aggregate()
            .asString()
            .mapNotNull(this::parseVersionFileAssetId)
            .flatMap(this::downloadVersionFile);
    }

    private Mono<String> downloadVersionFile(final String assetId) {
        return httpClient
            .followRedirect(true)
            .headers(h -> h.remove(HttpHeaderNames.ACCEPT))
            .headers(h -> h.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_OCTET_STREAM))
            .get()
            .uri("/releases/assets/" + assetId)
            .responseContent()
            .aggregate()
            .asString()
            .map(String::trim);
    }

    private String parseVersionFileAssetId(final String response) {
        try {
            JsonNode release = objectMapper.readTree(response);
            List<JsonNode> releaseNodes = release.findParents("name");
            releaseNodes.removeIf(node -> !node.get("name").textValue().matches("version.txt"));
            return releaseNodes.stream().findFirst()
                .map(node -> node.get("id").asText())
                .orElse(null);
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to parse version file asset ID", e);
        }
        return null;
    }
}
