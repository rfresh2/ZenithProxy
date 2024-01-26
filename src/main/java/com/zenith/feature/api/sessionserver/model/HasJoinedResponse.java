package com.zenith.feature.api.sessionserver.model;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.feature.api.ProfileData;

import java.util.List;
import java.util.UUID;

public record HasJoinedResponse(String id, String name, List<GameProfile.Property> properties) implements ProfileData {
    @Override
    public UUID uuid() {
        return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    public GameProfile toGameProfile() {
        var profile = new GameProfile(uuid(), name);
        profile.setProperties(properties);
        return profile;
    }
}
