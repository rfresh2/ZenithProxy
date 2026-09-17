package com.zenith;

import com.google.gson.Gson;
import com.zenith.util.config.Config;
import com.zenith.util.config.LaunchConfig;
import lombok.SneakyThrows;

import java.nio.file.Files;

public class TestUtils {

    @SneakyThrows
    public static void setConfigFile(Config config) {
        var tempConfigFile = Files.createTempFile("zenith-test-config", ".json");
        tempConfigFile.toFile().deleteOnExit();
        try (var writer = Files.newBufferedWriter(tempConfigFile)) {
            new Gson().toJson(config, writer);
        }
        System.setProperty("zenith.config.file", tempConfigFile.toAbsolutePath().toString());
    }

    @SneakyThrows
    public static void setLaunchConfigFile(LaunchConfig config) {
        var tempConfigFile = Files.createTempFile("zenith-test-launch-config", ".json");
        tempConfigFile.toFile().deleteOnExit();
        try (var writer = Files.newBufferedWriter(tempConfigFile)) {
            new Gson().toJson(config, writer);
        }
        System.setProperty("zenith.launch.config.file", tempConfigFile.toAbsolutePath().toString());
    }
}
