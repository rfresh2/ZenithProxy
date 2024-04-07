package com.zenith.feature.api;

import com.zenith.Shared;
import com.zenith.feature.api.crafthead.CraftheadApi;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CraftheadTest {

    public CraftheadApi api = new CraftheadApi();

//    @BeforeAll
    public static void setup() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
    }

//    @Test
    public void getProfileFromUsername() {
        var responseOptional = api.getProfile("rfresh2");
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("rfresh2", response.name());
        assertEquals(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"), response.uuid());
    }

//    @Test
    public void getProfileFromUUID() {
        var responseOptional = api.getProfile(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertEquals("rfresh2", response.name());
        assertEquals(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"), response.uuid());
    }

//    @Test
    public void getAvatarFromUsername() {
        var responseOptional = api.getAvatar("rfresh2");
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertTrue(response.length > 0);
    }

//    @Test
    public void getAvatarFromUUID() {
        var responseOptional = api.getAvatar(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertTrue(response.length > 0);
    }
}
