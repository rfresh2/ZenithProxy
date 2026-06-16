package com.zenith.feature.api.minetools;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.minetools.model.MinetoolsProfileResponse;
import com.zenith.feature.api.minetools.model.MinetoolsUuidResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * API no longer exists
 */
@Deprecated(forRemoval = true)
public class MinetoolsApi extends Api {
    public static final MinetoolsApi INSTANCE = new MinetoolsApi();

    public MinetoolsApi() {
        super("https://api.minetools.eu");
    }

    public Optional<MinetoolsUuidResponse> getProfileFromUsername(final String username) {
        return get("/uuid/" + username, MinetoolsUuidResponse.class).filter(r -> !"ERR".equals(r.status()));
    }

    public Optional<MinetoolsProfileResponse> getProfileFromUUID(final UUID uuid) {
        return get("/profile/" + uuid.toString(), MinetoolsProfileResponse.class);
    }
}
