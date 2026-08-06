package com.zenith.feature.api.mccompanion;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mccompanion.model.MCCompanionBedrockResponse;
import com.zenith.feature.api.mccompanion.model.MCCompanionJavaResponse;

import java.util.Optional;
import java.util.UUID;

public class MCCompanionApi extends Api {
    public static final MCCompanionApi INSTANCE = new MCCompanionApi();

    public MCCompanionApi() {
        super("https://api.mccompanion.net");
    }

    public Optional<MCCompanionBedrockResponse> getBedrockProfile(String gamertag) {
        return get("/api/lookup/bedrock/" + gamertag, MCCompanionBedrockResponse.class);
    }

    public Optional<MCCompanionBedrockResponse> getBedrockProfile(UUID uuid) {
        return get("/api/lookup/bedrock/" + xuidFromUUID(uuid), MCCompanionBedrockResponse.class);
    }

    public Optional<MCCompanionJavaResponse> getJavaProfile(String username) {
        return get("/api/lookup/java/" + username, MCCompanionJavaResponse.class);
    }

    public Optional<MCCompanionJavaResponse> getJavaProfile(UUID uuid) {
        return get("/api/lookup/java/" + uuid.toString(), MCCompanionJavaResponse.class);
    }

    static String xuidFromUUID(UUID uuid) {
        return Long.toUnsignedString(uuid.getLeastSignificantBits());
    }

}
