package com.zenith.feature.api.mcprofile.model;

import com.zenith.feature.api.ProfileData;

import java.util.UUID;

public record MCProfileBedrockResponse(
    String gamertag,
    String xuid,
    UUID floodgateuid,
    String icon,
    String skin,
    boolean linked,
    String java_uuid,
    String java_name
) implements ProfileData {
    @Override
    public String name() {
        return "." + gamertag;
    }

    @Override
    public UUID uuid() {
        return floodgateuid;
    }
}
