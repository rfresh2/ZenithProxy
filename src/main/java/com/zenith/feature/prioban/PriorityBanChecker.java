package com.zenith.feature.prioban;

import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.util.Optional;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DEFAULT_LOG;

public class PriorityBanChecker {
    private final HttpClient client;

    public PriorityBanChecker() {
        this.client = HttpClient.create()
                .followRedirect(false)
                .secure();
    }

    public Optional<Boolean> checkPrioBan() {
        try {
            HttpClientResponse response = client
                    .post()
                    .uri("https://shop.2b2t.org/checkout/packages/add/1994962/single?ign=" + CONFIG.authentication.username)
                    .response()
                    .block();
            try {
                String result = response.responseHeaders().get("Set-Cookie").split("; ")[0];
                if (result.contains("buycraft_basket")) { // unbanned
                    return Optional.of(false);
                } else if (result.contains("XRxlbOYKOzX5HYSsk7VO72KxURUxqkzYCSTxTat")) { // banned
                    return Optional.of(true);
                } else {
                    return Optional.empty();
                }
            } catch (final Throwable e) {
                DEFAULT_LOG.error("Unable to parse response cookies from 2b2t webstore", e);
            }
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error contacting 2b2t webstore", e);
        }
        return Optional.empty();
    }
}
