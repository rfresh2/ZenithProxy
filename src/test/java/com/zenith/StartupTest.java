package com.zenith;

import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.util.Wait;
import com.zenith.util.config.Config;
import com.zenith.util.config.LaunchConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestLogCaptureJunitExtension.class)
@DisabledInNativeImage
public class StartupTest {
    @Test
    public void launchZenithServer() {
        var launchThread = new AtomicReference<Thread>();
        try {
            var config = new Config();
            config.interactiveTerminal.enable = false;
            config.server.bind.port = 0;
            TestUtils.setConfigFile(config);

            var launchConfig = new LaunchConfig();
            launchConfig.auto_update = false;
            launchConfig.auto_update_launcher = false;
            TestUtils.setLaunchConfigFile(launchConfig);

            launchThread.set(Thread.ofPlatform().daemon().start(Proxy::main));

            assertTrue(Wait.waitUntil(() ->
                        !launchThread.get().isAlive()
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
        } finally {
            var t = launchThread.get();
            if (t != null) {
                t.interrupt();
            }
        }
    }
}
