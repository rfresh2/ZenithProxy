package com.zenith.feature.api;

import com.zenith.Shared;
import com.zenith.feature.api.model.PlaytimeResponse;
import com.zenith.feature.api.model.SeenResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VcApiTests {

    final VcApi api = new VcApi();


//    @BeforeAll
    public static void setup() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
    }

//    @Test
    public void seen() {
        SeenResponse seenResponse = api.getSeen("rfresh2").get();
        assertNotNull(seenResponse);
        System.out.println(seenResponse);
    }

//    @Test
    public void playtime() {
        PlaytimeResponse playtime = api.getPlaytime("rfresh2").get();
        assertNotNull(playtime);
        assertTrue(playtime.playtimeSeconds() > 0);
        System.out.println(playtime);
    }
}
