package com.zenith.feature.api.minetools.model;

import com.zenith.feature.api.ProfileData;

import java.util.UUID;

public record MinetoolsProfileResponse(MinetoolsProfileResponseDecoded decoded) implements ProfileData {
    public UUID uuid() {
        return UUID.fromString(decoded().profileId().replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    public String name() {
        return decoded.profileName();
    }
}
