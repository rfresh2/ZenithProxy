package com.zenith.feature.api.mcprofile.model;

import com.zenith.feature.api.ProfileData;

import java.util.UUID;

public record MCProfileJavaResponse(
    String username,
    UUID uuid,
    String skin,
    String cape,
    boolean linked
) implements ProfileData {
    @Override
    public String name() {
        return username();
    }
}
