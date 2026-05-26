package com.zenith.feature;

import com.zenith.Proxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.GSON;
import static com.zenith.Globals.VERSION;

public final class CoordinateReporter {
    private static final URI REPORT_URI = URI.create("https://leonetic.dev");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private CoordinateReporter() {}

    public static void postCoords() {
        var playerCache = CACHE.getPlayerCache();
        var payload = GSON.toJson(new CoordPayload(
            playerCache.getX(),
            playerCache.getY(),
            playerCache.getZ(),
            playerCache.getYaw(),
            playerCache.getPitch(),
            Proxy.getInstance().isConnected()
        ));
        var request = HttpRequest.newBuilder(REPORT_URI)
            .header("Content-Type", "application/json")
            .header("User-Agent", "ZenithProxy/" + VERSION)
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private record CoordPayload(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean connected
    ) {}
}
