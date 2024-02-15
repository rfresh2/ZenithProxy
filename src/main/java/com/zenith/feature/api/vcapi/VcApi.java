package com.zenith.feature.api.vcapi;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.vcapi.model.PlaytimeResponse;
import com.zenith.feature.api.vcapi.model.QueueResponse;
import com.zenith.feature.api.vcapi.model.SeenResponse;
import com.zenith.feature.api.vcapi.model.StatsResponse;

import java.util.Optional;

public class VcApi extends Api {

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
}
