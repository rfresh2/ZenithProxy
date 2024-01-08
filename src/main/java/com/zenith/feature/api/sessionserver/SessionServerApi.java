package com.zenith.feature.api.sessionserver;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.sessionserver.model.SessionProfileResponse;

import java.util.Optional;
import java.util.UUID;

public class SessionServerApi extends Api {
    public SessionServerApi() {
        super("https://sessionserver.mojang.com");
    }


    public Optional<SessionProfileResponse> getProfileFromUUID(final UUID uuid) {
        return get("/session/minecraft/profile/" + uuid.toString(), SessionProfileResponse.class);
    }
}
