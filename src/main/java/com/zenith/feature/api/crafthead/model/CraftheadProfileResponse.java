package com.zenith.feature.api.crafthead.model;

import com.zenith.feature.api.ProfileData;

import java.util.List;
import java.util.UUID;

public record CraftheadProfileResponse(
    String id,
    String name,
    List<CraftheadProfileProperties> properties
) implements ProfileData {
    @Override
    public UUID uuid() {
        return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
