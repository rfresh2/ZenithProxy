package com.zenith.feature.api.vcapi;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.vcapi.model.*;

import java.util.Optional;

public class VcApi extends Api {
    public static VcApi INSTANCE = new VcApi();

    public VcApi() {
        super("https://api.2b2t.vc");
    }

    public Optional<SeenResponse> getSeen(final String playerName) {
        return get("/seen?playerName=" + playerName, SeenResponse.class);
    }

    public Optional<PlaytimeResponse> getPlaytime(final String playerName) {
        return get("/playtime?playerName=" + playerName, PlaytimeResponse.class);
    }

    public Optional<StatsResponse> getStats(final String playerName) {
        return get("/stats/player?playerName=" + playerName, StatsResponse.class);
    }

    public Optional<QueueResponse> getQueue() {
        return get("/queue", QueueResponse.class);
    }

    public Optional<QueueEtaEquationResponse> getQueueEtaEquation() {
        return get("/queue/eta-equation", QueueEtaEquationResponse.class);
    }
}
