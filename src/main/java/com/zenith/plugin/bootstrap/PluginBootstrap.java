package com.zenith.plugin.bootstrap;

import com.zenith.plugin.PluginDiscovery;
import com.zenith.plugin.api.PluginInfo;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Data bridge between the JVM bootstrap loader and the application loader.
 */
@NullMarked
public final class PluginBootstrap {
    // null if launched through legacy com.zenith.Proxy.main
    public static volatile @Nullable List<PluginDiscovery.DiscoveredPlugin> discoveredPlugins;

    // updated by shared classloader when transformation must be disabled
    public static volatile Set<String> disabledIds = Set.of();

    private PluginBootstrap() {}

    /**
     * Installs metadata discovered by the JVM bootstrap
     */
    public static void install(String[] metadataJson, String[] paths, Set<String> disabledIds) {
        Objects.requireNonNull(metadataJson, "metadataJson");
        Objects.requireNonNull(paths, "paths");
        Objects.requireNonNull(disabledIds, "disabledIds");
        if (metadataJson.length != paths.length) {
            throw new IllegalArgumentException(
                "Plugin metadata and path counts differ: " + metadataJson.length + " != " + paths.length
            );
        }

        var installed = new ArrayList<PluginDiscovery.DiscoveredPlugin>(metadataJson.length);
        for (var i = 0; i < metadataJson.length; i++) {
            var metadata = Objects.requireNonNull(metadataJson[i], "metadataJson[" + i + "]");
            var path = Objects.requireNonNull(paths[i], "paths[" + i + "]");
            PluginInfo info;
            try (var stream = new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8))) {
                info = PluginDiscovery.readInfo(stream);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to install plugin metadata at index " + i, e);
            }
            installed.add(new PluginDiscovery.DiscoveredPlugin(info, Path.of(path)));
        }

        PluginBootstrap.discoveredPlugins = installed;
        PluginBootstrap.disabledIds = disabledIds;
    }
}
