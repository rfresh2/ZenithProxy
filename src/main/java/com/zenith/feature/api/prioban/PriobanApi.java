package com.zenith.feature.api.prioban;

import com.zenith.feature.api.Api;

import java.util.Optional;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DEFAULT_LOG;

public class PriobanApi extends Api {
    public PriobanApi() {
        super("https://shop.2b2t.org");
    }

    public Optional<Boolean> checkPrioBan() {
        var response = post("/checkout/packages/add/1994962/single?ign=" + CONFIG.authentication.username);
        if (response.isPresent()) {
            var r = response.get();
            try {
                var cookie = r.headers().map().get("Set-Cookie").getFirst().split("; ")[0];
                if (cookie.contains("buycraft_basket"))
                    return Optional.of(false);
                else if (cookie.contains("XRxlbOYKOzX5HYSsk7VO72KxURUxqkzYCSTxTat"))
                    return Optional.of(true);
                else
                    return Optional.empty();
            } catch (final Exception e) {
                DEFAULT_LOG.error("Unable to parse response cookies from 2b2t webstore", e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
