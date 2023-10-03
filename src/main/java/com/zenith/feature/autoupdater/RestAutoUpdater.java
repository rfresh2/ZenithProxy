package com.zenith.feature.autoupdater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.math3.util.Pair;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.zenith.Shared.DEFAULT_LOG;
import static com.zenith.Shared.LAUNCH_CONFIG;

public class RestAutoUpdater extends AutoUpdater {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestAutoUpdater() {
        String baseUrl = LAUNCH_CONFIG.repo_owner.equals("rfresh2") && LAUNCH_CONFIG.repo_name.equals("ZenithProxy")
            ? "https://github.2b2t.vc"
            : "https://api.github.com";
        this.httpClient = HttpClient.create()
            .secure()
            .baseUrl(baseUrl + "/repos/" + LAUNCH_CONFIG.repo_owner + "/" + LAUNCH_CONFIG.repo_name)
            .headers(h -> h.add(HttpHeaderNames.USER_AGENT, "ZenithProxy/" + LAUNCH_CONFIG.version))
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
        return Stream.of("git", "java", "linux")
            .anyMatch(in::startsWith);
    }

    @Override
    public void updateCheck() {
        // skip if we already found an update
        // there are rate limits on the github api so its best to avoid calls where not needed
        if (getUpdateAvailable()) return;
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
                    if (!Objects.equals(LAUNCH_CONFIG.version, releaseIdToTag.getSecond()) && versionIsLessThanCurrent(LAUNCH_CONFIG.version, releaseIdToTag.getSecond())) {
                        if (!getUpdateAvailable()) {
                            DEFAULT_LOG.info(
                                "New update on release channel {}! Current: {} New: {}!",
                                LAUNCH_CONFIG.release_channel,
                                LAUNCH_CONFIG.version,
                                releaseIdToTag.getSecond());
                            setUpdateAvailable(true, releaseIdToTag.getSecond());
                        }
                    }
                } else DEFAULT_LOG.warn("Invalid version on release: '{}'", releaseIdToTag.getSecond());
                return Mono.empty();
            })
            .block();
    }

    private boolean versionLooksCorrect(final String version) {
        return version != null && version.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\+.*") && version.endsWith("+" + LAUNCH_CONFIG.release_channel);
    }

    private boolean versionIsLessThanCurrent(final String current, final String newVersion) {
        String[] currentSplit = current.split("\\.");
        String[] newSplit = newVersion.split("\\.");
        // replace any non-numeric characters with empty string
        for (int i = 0; i < 3; i++) {
            try {
                currentSplit[i] = currentSplit[i].replaceAll("[^\\d]", "");
                if (currentSplit[i].isEmpty()) {
                    currentSplit[i] = "0";
                }
                int currentInt = Integer.parseInt(currentSplit[i]);
                newSplit[i] = newSplit[i].replaceAll("[^\\d]", "");
                if (newSplit[i].isEmpty()) {
                    newSplit[i] = "0";
                }
                int newInt = Integer.parseInt(newSplit[i]);
                if (currentInt > newInt) {
                    return false;
                } else if (newInt > currentInt) {
                    return true;
                }
            } catch (final Exception e) {
                DEFAULT_LOG.error("Failed to parse version: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }

    private Pair<String, String> parseLatestReleaseId(String response) {
        try {
            JsonNode releases = objectMapper.readTree(response);

            List<JsonNode> releaseNodes = releases.findParents("tag_name");
            releaseNodes.removeIf(node -> node.get("draft").asBoolean());
            releaseNodes.removeIf(node -> !node.get("tag_name").textValue().endsWith("+" + LAUNCH_CONFIG.release_channel));

            releaseNodes.sort((a, b) -> b.get("published_at").asText().compareTo(a.get("published_at").asText()));

            if (!releaseNodes.isEmpty()) {
                return Pair.create(releaseNodes.get(0).get("id").asText(), releaseNodes.get(0).get("tag_name").asText());
            }
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to parse latest release ID.", e);
        }

        return null;
    }
}
