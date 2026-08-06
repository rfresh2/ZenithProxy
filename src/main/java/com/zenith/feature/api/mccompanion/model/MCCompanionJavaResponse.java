package com.zenith.feature.api.mccompanion.model;

import com.zenith.feature.api.ProfileData;

import java.util.UUID;

public record MCCompanionJavaResponse(
    String username,
    UUID uuid,
    String skinUrl
) implements ProfileData {

    @Override
    public String name() {
        return username;
    }
}
