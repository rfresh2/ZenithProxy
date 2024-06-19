package com.zenith.feature.api;

import com.zenith.Shared;
import com.zenith.feature.api.mcstatus.MCStatusApi;
import com.zenith.feature.api.mcstatus.model.MCStatusResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MCStatusApiTest {

//    @BeforeAll
    public static void before() {
        Shared.loadLaunchConfig();
        Shared.loadConfig();
    }

//    @Test
    public void test() {
        Optional<MCStatusResponse> responseOptional = MCStatusApi.INSTANCE.getMCServerStatus("connect.2b2t.org");
        assertTrue(responseOptional.isPresent());
        var response = responseOptional.get();
        assertTrue(response.online());
    }
}
