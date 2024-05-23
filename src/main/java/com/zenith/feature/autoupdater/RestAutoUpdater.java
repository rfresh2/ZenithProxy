package com.zenith.feature.autoupdater;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenith.util.Pair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.zenith.Shared.*;

public class RestAutoUpdater extends AutoUpdater {
    private final HttpClient httpClient;
    private final String baseUrl;

    public RestAutoUpdater() {
        this.baseUrl = LAUNCH_CONFIG.repo_owner.equals("rfresh2") && LAUNCH_CONFIG.repo_name.equals("ZenithProxy")
            ? "https://github.2b2t.vc"
            : "https://api.github.com";
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
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
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/repos/" + LAUNCH_CONFIG.repo_owner + "/" + LAUNCH_CONFIG.repo_name + "/releases?per_page=100"))
            .headers("User-Agent", "ZenithProxy/" + LAUNCH_CONFIG.version)
            .headers("Accept", "application/vnd.github+json")
            .headers("X-GitHub-Api-Version", "2022-11-28")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            Pair<String, String> releaseIdToTag = parseLatestReleaseId(responseBody);
            if (releaseIdToTag == null || releaseIdToTag.left() == null || releaseIdToTag.right() == null) {
                return;
            }
            if (versionLooksCorrect(releaseIdToTag.right())) {
                if (!Objects.equals(LAUNCH_CONFIG.version, releaseIdToTag.right()) && versionIsLessThanCurrent(LAUNCH_CONFIG.version, releaseIdToTag.right())) {
                    if (!getUpdateAvailable()) {
                        DEFAULT_LOG.info(
                            "New update on release channel {}! Current: {} New: {}!",
                            LAUNCH_CONFIG.release_channel,
                            LAUNCH_CONFIG.version,
                            releaseIdToTag.right());
                    }
                    setUpdateAvailable(true, releaseIdToTag.right());
                }
            } else DEFAULT_LOG.warn("Invalid version on release: '{}'", releaseIdToTag.right());
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to check for updates: {}", e.getMessage());
        }
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
            JsonNode releases = OBJECT_MAPPER.readTree(response);

            List<JsonNode> releaseNodes = releases.findParents("tag_name");
            releaseNodes.removeIf(node -> node.get("draft").asBoolean());
            releaseNodes.removeIf(node -> !node.get("tag_name").textValue().endsWith("+" + LAUNCH_CONFIG.release_channel));

            releaseNodes.sort((a, b) -> b.get("published_at").asText().compareTo(a.get("published_at").asText()));

            if (!releaseNodes.isEmpty()) {
                return Pair.of(releaseNodes.get(0).get("id").asText(), releaseNodes.get(0).get("tag_name").asText());
            }
        } catch (Throwable e) {
            DEFAULT_LOG.error("Failed to parse latest release ID.", e);
        }

        return null;
    }
}
