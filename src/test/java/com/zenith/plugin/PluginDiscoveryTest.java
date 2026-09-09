package com.zenith.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisabledInNativeImage
class PluginDiscoveryTest {
    @TempDir
    Path temporaryDirectory;

    PluginDiscovery.PluginDiscoveryLogSink logSink = (m, e) -> {
        System.out.println(m);
        if (e != null) {
            e.printStackTrace();
        }
    };

    @Test
    void missingMixinsAreNormalizedAndClasspathPluginsAreDiscovered() throws Exception {
        var info = PluginDiscovery.readInfo(new ByteArrayInputStream(metadata("classpath-plugin", "1.0.0", false)
            .getBytes(StandardCharsets.UTF_8)));
        assertNotNull(info.mixins());
        assertEquals(List.of(), info.mixins());

        var classpathRoot = temporaryDirectory.resolve("classpath");
        Files.createDirectories(classpathRoot);
        Files.writeString(classpathRoot.resolve(PluginDiscovery.PLUGIN_INFO_FILE),
            metadata("classpath-plugin", "1.0.0", false));
        try (var classpathLoader = new URLClassLoader(new URL[]{classpathRoot.toUri().toURL()}, null)) {
            var discovered = PluginDiscovery.discover(
                temporaryDirectory.resolve("plugins"),
                classpathLoader,
                "1.21.4",
                logSink
            );
            assertEquals(1, discovered.size());
            assertEquals("classpath-plugin", discovered.getFirst().info().id());
            assertEquals(Path.of(""), discovered.getFirst().path());
            assertEquals(discovered, PluginDiscovery.discoverClasspath(classpathLoader, "1.21.4", logSink));
        }
    }

    @Test
    void readsLegacyJarMetadataAndSelectsHighestDuplicateVersion() throws Exception {
        var plugins = temporaryDirectory.resolve("plugins");
        Files.createDirectories(plugins);
        createPluginJar(plugins.resolve("old.jar"), "example", "1.0.0", PluginDiscovery.PLUGIN_INFO_FILE);
        createPluginJar(plugins.resolve("new.jar"), "example", "2.0.0", PluginDiscovery.LEGACY_PLUGIN_INFO_FILE);

        try (var classpathLoader = new URLClassLoader(new URL[0], null)) {
            var discovered = PluginDiscovery.discover(plugins,
                classpathLoader,
                "1.21.4",
                logSink
            );

            assertEquals(1, discovered.size());
            assertEquals("2.0.0", discovered.getFirst().info().version().toString());
            assertEquals(plugins.resolve("new.jar"), discovered.getFirst().path());
        }
    }

    private static String metadata(String id, String version, boolean includeMixins) {
        return """
            {
              "entrypoint": "example.Plugin",
              "id": "%s",
              "version": "%s",
              "description": "Test plugin",
              "url": "",
              "authors": [],
              "mcVersions": ["1.21.4"]%s
            }
            """.formatted(id, version, includeMixins ? ",\n  \"mixins\": []" : "");
    }

    private static void createPluginJar(Path path, String id, String version, String metadataName) throws IOException {
        try (var output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry(metadataName));
            output.write(metadata(id, version, false).getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
