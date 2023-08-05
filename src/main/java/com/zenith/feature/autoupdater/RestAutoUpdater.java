package com.zenith.feature.autoupdater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.Proxy;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.math3.util.Pair;
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
        return List.of("git", "java", "linux", "linux.pre").contains(in);
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
                Pair<String, String> releaseIdToTag = parseLatestReleaseId(response);
                if (releaseIdToTag == null || releaseIdToTag.getFirst() == null || releaseIdToTag.getSecond() == null) {
                    return Mono.empty();
                }
                if (versionLooksCorrect(releaseIdToTag.getSecond())) {
                    if (!Objects.equals(LAUNCH_CONFIG.version, releaseIdToTag.getSecond())) {
                        if (!getUpdateAvailable()) DEFAULT_LOG.info(
                            "New update on release channel {}! Current: {} New: {}!",
                            LAUNCH_CONFIG.release_channel,
                            Proxy.getVersion(),
                            releaseIdToTag.getSecond());
                        setUpdateAvailable(true);
                    }
                } else DEFAULT_LOG.warn("Invalid version on release: '{}'", releaseIdToTag.getSecond());
                return Mono.empty();
            })
            .block();
    }

    private boolean versionLooksCorrect(final String version) {
        return version != null && version.matches("[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    private Pair<String, String> parseLatestReleaseId(String response) {
        try {
            JsonNode releases = objectMapper.readTree(response);

            List<JsonNode> releaseNodes = releases.findParents("tag_name");
            releaseNodes.removeIf(node -> !node.get("tag_name").textValue().endsWith("+" + LAUNCH_CONFIG.release_channel));

            releaseNodes.sort((a, b) -> b.get("published_at").asText().compareTo(a.get("published_at").asText()));

            if (!releaseNodes.isEmpty()) {
                return Pair.create(releaseNodes.get(0).get("id").asText(), releaseNodes.get(0).get("tag_name").asText().split("\\+")[0]);
            }
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to parse latest release ID.", e);
        }

        return null;
    }
}
