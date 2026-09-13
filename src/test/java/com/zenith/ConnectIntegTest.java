package com.zenith;

import com.zenith.util.Wait;
import com.zenith.util.config.Config;
import com.zenith.util.config.LaunchConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(TestLogCaptureJunitExtension.class)
@DisabledInNativeImage
public class ConnectIntegTest {

    @Container
    public GenericContainer minecraftServer = new GenericContainer(DockerImageName.parse("itzg/minecraft-server:java21"))
        .withExposedPorts(25565)
        .withEnv("EULA", "TRUE")
        .withEnv("TYPE", "PAPER")
        .withEnv("VERSION", "1.21.4")
        .withEnv("ONLINE_MODE", "FALSE")
        .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forLogMessage(".*Done \\(.*\\)!.*", 1))
        .withStartupTimeout(Duration.ofMinutes(3));

    @Test
    public void connectTest() {
        var launchThread = new AtomicReference<Thread>();
        try {
            var config = new Config();
            config.interactiveTerminal.enable = false;
            config.server.bind.port = 0;
            config.client.server.address = "localhost";
            config.client.server.port = minecraftServer.getFirstMappedPort();
            config.authentication.accountType = Config.Authentication.AccountType.OFFLINE;
            config.authentication.username = "ZenithTest";
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

            Proxy.getInstance().connectAndCatchExceptions();

            assertTrue(Proxy.getInstance().isConnected(), "Failed to connect to local mc server: " + minecraftServer.getLogs());
        } finally {
            var t = launchThread.get();
            if (t != null) {
                t.interrupt();
            }
        }
    }
}
