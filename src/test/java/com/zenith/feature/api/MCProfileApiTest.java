package com.zenith.feature.api;

import com.zenith.feature.api.mcprofile.MCProfileApi;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MCProfileApiTest {

//    @Test
    public void testBedrockProfileLookupByGamertag() {
        final String gamertag = "Dream";
        var responseOptional = MCProfileApi.INSTANCE.getBedrockProfile(gamertag);
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals(gamertag, response.gamertag());
        assertEquals("Dream", response.gamertag());
        assertEquals("Dream", response.name());
        assertEquals(UUID.fromString("00000000-0000-0000-0009-01f2496167c9"), response.uuid());
        assertFalse(response.linked());
    }

//    @Test
    public void testBedrockProfileLookupByUUID() {
        final UUID uuid = UUID.fromString("00000000-0000-0000-0009-01f2496167c9");
        var responseOptional = MCProfileApi.INSTANCE.getBedrockProfile(uuid);
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("Dream", response.gamertag());
        assertEquals("Dream", response.gamertag());
        assertEquals("Dream", response.name());
        assertEquals(uuid, response.uuid());
    }
}
