package com.zenith.plugin;

import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import com.zenith.util.ImageInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluginManagerNativeClasspathTest {
    @Test
    void nativeImageRuntimeLoadsBundledPluginAndSkipsExternalJars() throws Exception {
        var imageCodeProperty = ImageInfo.PROPERTY_IMAGE_CODE_KEY;
        var previousImageCode = System.getProperty(imageCodeProperty);
        Files.createDirectories(PluginManager.PLUGINS_PATH);
        var externalJar = Files.createTempFile(PluginManager.PLUGINS_PATH, "native-classpath-test-", ".jar");
        writeExternalPluginJar(externalJar);
        try {
            System.setProperty(imageCodeProperty, ImageInfo.PROPERTY_IMAGE_CODE_VALUE_RUNTIME);

            var manager = new PluginManager();
            manager.initialize();

            assertNotNull(manager.getPlugin("native-classpath-test"));
            assertNull(manager.getPluginInstance("native-external-test"));
        } finally {
            Files.deleteIfExists(externalJar);
            if (previousImageCode == null) System.clearProperty(imageCodeProperty);
            else System.setProperty(imageCodeProperty, previousImageCode);
        }
    }

    private static void writeExternalPluginJar(Path path) throws IOException {
        var metadata = """
            {
              "entrypoint": "missing.ExternalPlugin",
              "id": "native-external-test",
              "version": "1.0.0",
              "description": "External test plugin",
              "url": "",
              "authors": [],
              "mcVersions": ["1.21.4"]
            }
            """;
        try (var output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry(PluginDiscovery.PLUGIN_INFO_FILE));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    public static final class ClasspathTestPlugin implements ZenithProxyPlugin {
        @Override
        public void onLoad(PluginAPI pluginAPI) {
        }
    }
}
