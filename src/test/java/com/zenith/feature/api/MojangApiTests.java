package com.zenith.feature.api;

import com.zenith.Shared;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.mojang.MojangApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MojangApiTests {

    final MojangApi api = new MojangApi();
    final MinetoolsApi minetoolsApi = new MinetoolsApi();
    final SessionServerApi sessionServerApi = new SessionServerApi();

//    @BeforeAll
    public static void setup() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
    }

//    @Test
    public void getMojangProfileTest() {
        var response = api.getProfileFromUsername("rfresh2");
        assertTrue(response.isPresent());
        assertTrue(response.get().name().equals("rfresh2"));
        var uuid = response.get().uuid();
    }

//    @Test
    public void getMinetoolsProfileTest() {
        var response = minetoolsApi.getProfileFromUsername("rfresh2");
        assertTrue(response.isPresent());
        assertTrue(response.get().name().equals("rfresh2"));
        var uuid = response.get().uuid();
    }

//    @Test
    public void getSessionServerProfileTest() {
        var response = sessionServerApi.getProfileFromUUID(UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892"));
        assertTrue(response.isPresent());
        assertTrue(response.get().name().equals("rfresh2"));
        var uuid = response.get().uuid();
    }
}
