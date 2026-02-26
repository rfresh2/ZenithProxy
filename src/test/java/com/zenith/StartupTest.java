package com.zenith;

import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.util.Wait;
import com.zenith.util.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestLogCaptureJunitExtension.class)
public class StartupTest {
    @Test
    public void launchZenithServer() throws Exception {
        var config = new Config();
        config.interactiveTerminal.enable = false;
        config.server.bind.port = 0;
        TestUtils.setConfigFile(config);

        var launchThread = Thread.ofPlatform().start(Proxy::main);

        assertTrue(Wait.waitUntil(() ->
            !launchThread.isAlive()
            || (Proxy.getInstance().getServer() != null && Proxy.getInstance().getServer().isListening()),
            10),
            "Failed to start Zenith server"
        );

        try {
            var response = MCPing.INSTANCE.ping("localhost", Proxy.getInstance().getServer().getPort(), 5000, false);
            assertEquals("ZenithProxy", response.version().name());
        } catch (Exception e) {
            fail("Failed to ping local Zenith mc server", e);
        }
        Proxy.getInstance().stop();
        launchThread.join(5000);
    }
}
