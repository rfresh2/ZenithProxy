package com.zenith.plugin;

import com.zenith.plugin.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zenith.Globals.PLUGIN_MANAGER;
import static com.zenith.Globals.saveConfig;
import static org.junit.jupiter.api.Assertions.*;

public class PluginInstanceTest {

    @Test
    public void testPluginInstanceLoad() throws IOException {
        var pluginInfo = new PluginInfo(
            TestPlugin.class.getName(),
            "test-plugin",
            new Version(1, 0, 0),
            "A test plugin for unit testing",
            "",
            Collections.emptyList(),
            List.of("*")
        );
        PLUGIN_MANAGER.preLoadPluginInstance(pluginInfo, Path.of("test-plugin.jar"), this.getClass().getClassLoader());
        var pluginInstance = PLUGIN_MANAGER.getPluginInstances().stream().filter(i -> i.getId().equals(pluginInfo.id())).findFirst().orElseThrow();
        PLUGIN_MANAGER.loadPlugin(pluginInstance);
        assertNotNull(PLUGIN_MANAGER.getPlugin("test-plugin"));
        assertEquals(1, TestPlugin.onLoadCounter.get());
        saveConfig();
        Path testConfigPath = Path.of("plugins/config/test-plugin-config.json");
        assertTrue(Files.exists(testConfigPath), "Plugin config file should be created");
        Files.delete(testConfigPath);
    }

    @Plugin(id = "test-plugin")
    public static class TestPlugin implements ZenithProxyPlugin {
        public static AtomicInteger onLoadCounter = new AtomicInteger(0);

        @Override
        public void onLoad(final PluginAPI pluginAPI) {
            onLoadCounter.incrementAndGet();
            TestPluginConfig config = pluginAPI.registerConfig("test-plugin-config", TestPluginConfig.class);
            assertNotNull(config);
            assertEquals("exampleValue", config.exampleField);
        }
    }

    public static class TestPluginConfig {
        public String exampleField = "exampleValue";
    }
}
