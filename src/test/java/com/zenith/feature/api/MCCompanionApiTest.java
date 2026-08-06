package com.zenith.feature.api;

import com.zenith.feature.api.mccompanion.MCCompanionApi;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MCCompanionApiTest {
//    @Test
    public void testBedrockProfileLookupByGamertag() {
        final String gamertag = "Dream";
        var responseOptional = MCCompanionApi.INSTANCE.getBedrockProfile(gamertag);
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals(gamertag, response.gamertag());
        assertEquals("Dream", response.gamertag());
        assertEquals(".Dream", response.name());
        assertEquals(UUID.fromString("00000000-0000-0000-0009-01f2496167c9"), response.uuid());
    }

//    @Test
    public void testBedrockProfileLookupByUUID() {
        final UUID uuid = UUID.fromString("00000000-0000-0000-0009-01f2496167c9");
        var responseOptional = MCCompanionApi.INSTANCE.getBedrockProfile(uuid);
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("Dream", response.gamertag());
        assertEquals(".Dream", response.name());
        assertEquals(uuid, response.uuid());
    }

//    @Test
    public void getJavaProfileFromUsername() {
        var responseOptional = MCCompanionApi.INSTANCE.getJavaProfile("rfresh2");
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("rfresh2", response.name());
        assertEquals(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"), response.uuid());
    }

//    @Test
    public void getJavaProfileFromUUID() {
        var responseOptional = MCCompanionApi.INSTANCE.getJavaProfile(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("rfresh2", response.name());
        assertEquals(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"), response.uuid());
    }
}
