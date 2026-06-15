package com.zenith.feature.api.mcprofile;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mcprofile.model.MCProfileBedrockResponse;
import com.zenith.feature.api.mcprofile.model.MCProfileJavaResponse;

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

    public Optional<MCProfileJavaResponse> getJavaProfile(final String username) {
        return get("/java/username/" + username, MCProfileJavaResponse.class);
    }

    public Optional<MCProfileJavaResponse> getJavaProfile(final UUID uuid) {
        return get("/java/uuid/" + uuid.toString(), MCProfileJavaResponse.class);
    }

    static String xuidFromUUID(UUID uuid) {
        return Long.toUnsignedString(uuid.getLeastSignificantBits());
    }
}
