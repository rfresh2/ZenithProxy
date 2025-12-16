package com.zenith.feature.api.mcprofile;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mcprofile.model.MCProfileBedrockResponse;

import java.util.Optional;
import java.util.UUID;

public class MCProfileApi extends Api {
    public static final MCProfileApi INSTANCE = new MCProfileApi();

    public MCProfileApi() {
        super("https://mcprofile.io/api/v1");
    }

    public Optional<MCProfileBedrockResponse> getBedrockProfile(final String gamertag) {
        return get("/bedrock/gamertag/" + gamertag, MCProfileBedrockResponse.class);
    }

    public Optional<MCProfileBedrockResponse> getBedrockProfile(final UUID uuid) {
        return get("/bedrock/xuid/" + xuidFromUUID(uuid), MCProfileBedrockResponse.class);
    }

    static String xuidFromUUID(UUID uuid) {
        return Long.toUnsignedString(uuid.getLeastSignificantBits());
    }
}
