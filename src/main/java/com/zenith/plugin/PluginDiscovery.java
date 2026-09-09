package com.zenith.plugin;

import com.zenith.plugin.api.PluginInfo;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

import static java.util.Objects.requireNonNull;

public final class PluginDiscovery {
    public static final String PLUGIN_INFO_FILE = "zenithproxy.plugin.json";
    public static final String LEGACY_PLUGIN_INFO_FILE = "plugin.json";

    private static final Path CLASSPATH_PLUGIN_PATH = Path.of("");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PluginDiscovery() {}

    // dynamic log dispatch for bootstrap and application envs
    public interface PluginDiscoveryLogSink {
        void log(String message, @Nullable Throwable error);
    }

    public static List<DiscoveredPlugin> discover(
        Path pluginsPath,
        ClassLoader classpathLoader,
        String mcVersion,
        PluginDiscoveryLogSink logSink
    ) {
        requireNonNull(pluginsPath, "pluginsPath");
        requireNonNull(classpathLoader, "classpathLoader");

        var selected = new LinkedHashMap<String, DiscoveredPlugin>();
        discoverClasspathPlugins(classpathLoader, mcVersion, selected, logSink);
        discoverJarPlugins(pluginsPath, mcVersion, selected, logSink);
        return List.copyOf(selected.values());
    }

    public static PluginInfo readInfo(InputStream stream) {
        requireNonNull(stream, "stream");

        final PluginInfo info;
        try {
            info = OBJECT_MAPPER.readValue(stream, PluginInfo.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read plugin metadata", e);
        }

        validateInfo(info);
        return info;
    }

    public static List<DiscoveredPlugin> discoverClasspath(ClassLoader classpathLoader, String mcVersion, PluginDiscoveryLogSink logSink) {
        requireNonNull(classpathLoader, "classpathLoader");
        var selected = new LinkedHashMap<String, DiscoveredPlugin>();
        discoverClasspathPlugins(classpathLoader, mcVersion, selected, logSink);
        return List.copyOf(selected.values());
    }

    private static void discoverClasspathPlugins(
        ClassLoader classpathLoader,
        String mcVersion,
        Map<String, DiscoveredPlugin> selected,
        PluginDiscoveryLogSink logSink
    ) {
        final List<URL> resources;
        try {
            var resourceEnumeration = classpathLoader.getResources(PLUGIN_INFO_FILE);
            resources = Collections.list(resourceEnumeration);
            resources.sort(Comparator.comparing(URL::toExternalForm));
        } catch (Throwable e) {
            logSink.log("Unable to scan classpath plugin metadata", e);
            return;
        }

        for (var resource : resources) {
            try (var stream = resource.openStream()) {
                var info = readInfo(stream);
                if (!isCompatible(info, CLASSPATH_PLUGIN_PATH, mcVersion, logSink)) continue;
                select(new DiscoveredPlugin(info, CLASSPATH_PLUGIN_PATH), selected, logSink);
            } catch (Throwable e) {
                logSink.log("Unable to read classpath plugin metadata from " + resource, e);
            }
        }
    }

    private static void discoverJarPlugins(
        Path pluginsPath,
        String mcVersion,
        Map<String, DiscoveredPlugin> selected,
        PluginDiscoveryLogSink logSink
    ) {
        if (!Files.isDirectory(pluginsPath)) return;

        var jars = new ArrayList<Path>();
        try (var paths = Files.newDirectoryStream(
            pluginsPath,
            path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")
        )) {
            for (var path : paths) {
                jars.add(path);
            }
        } catch (Throwable e) {
            logSink.log("Unable to scan plugins directory " + pluginsPath, e);
            return;
        }
        jars.sort(Comparator.comparing(path -> path.getFileName().toString()));

        for (var jarPath : jars) {
            try {
                var info = readJarInfo(jarPath, logSink);
                if (!isCompatible(info, jarPath, mcVersion, logSink)) {
                    continue;
                }
                select(new DiscoveredPlugin(info, jarPath), selected, logSink);
            } catch (Throwable e) {
                logSink.log("Unable to read plugin metadata from " + jarPath, e);
            }
        }
    }

    private static PluginInfo readJarInfo(Path jarPath, PluginDiscoveryLogSink logSink) throws IOException {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(PLUGIN_INFO_FILE);
            var legacy = false;
            if (entry == null) {
                entry = jar.getJarEntry(LEGACY_PLUGIN_INFO_FILE);
                legacy = true;
            }
            if (entry == null) {
                throw new IllegalArgumentException(
                    "No " + PLUGIN_INFO_FILE + " or " + LEGACY_PLUGIN_INFO_FILE + " found"
                );
            }
            if (entry.isDirectory()) {
                throw new IllegalArgumentException("Plugin metadata entry is a directory: " + entry.getName());
            }
            if (legacy) {
                logSink.log("""
                    Plugin %s uses deprecated %s
                    rebuild it to migrate to %s"""
                    .formatted(jarPath, LEGACY_PLUGIN_INFO_FILE, PLUGIN_INFO_FILE),
                    null
                );
            }
            try (var stream = jar.getInputStream(entry)) {
                return readInfo(stream);
            }
        }
    }

    private static boolean isCompatible(PluginInfo info, Path path, String mcVersion, PluginDiscoveryLogSink logSink) {
        if (info.mcVersions().isEmpty()) {
            logSink.log("Skipping plugin %s: no MC versions specified".formatted(path), null);
            return false;
        }
        if (!info.mcVersions().contains("*")
            && (mcVersion == null || !info.mcVersions().contains(mcVersion))) {
            logSink.log("""
                Skipping plugin %s at %s: incompatible MC version (current: %s, required: %s)"""
                .formatted(info.id(), path, mcVersion, info.mcVersions()), null);
            return false;
        }
        return true;
    }

    private static void select(
        DiscoveredPlugin candidate,
        Map<String, DiscoveredPlugin> selected,
        PluginDiscoveryLogSink logSink
    ) {
        var id = candidate.info().id();
        var existing = selected.get(id);
        if (existing == null) {
            selected.put(id, candidate);
            return;
        }

        var comparison = existing.info().version().compareTo(candidate.info().version());
        if (comparison < 0) {
            logSink.log("Replacing plugin %s version %s from %s with newer version %s from %s"
                .formatted(id, existing.info().version(), existing.path(), candidate.info().version(), candidate.path()), null);
            selected.put(id, candidate);
        } else {
            logSink.log("Ignoring duplicate plugin %s version %s from %s; selected version is %s from %s"
                .formatted(id, candidate.info().version(), candidate.path(), existing.info().version(), existing.path()), null);
        }
    }

    private static void validateInfo(PluginInfo info) {
        requireNonNull(info, "Plugin info is null");
        requireNonNull(info.entrypoint(), "Entrypoint is null");
        if (info.entrypoint().isBlank()) throw new IllegalArgumentException("Invalid entrypoint");
        requireNonNull(info.id(), "Plugin id is null");
        if (info.id().isBlank()) throw new IllegalArgumentException("Invalid plugin id");
        if (!PluginInfo.ID_PATTERN.matcher(info.id()).matches()) {
            throw new IllegalArgumentException("Invalid plugin id: " + info.id());
        }
        requireNonNull(info.version(), "Plugin version is null");
        requireNonNull(info.description(), "Plugin description is null");
        requireNonNull(info.url(), "Plugin url is null");
        requireNonNull(info.authors(), "Plugin authors is null");
        requireNonNull(info.mcVersions(), "Plugin mcVersions is null");
        requireNonNull(info.mixins(), "Plugin mixins is null");
    }

    public record DiscoveredPlugin(PluginInfo info, Path path) {
        public DiscoveredPlugin {
            requireNonNull(info, "info");
            requireNonNull(path, "path");
        }
    }
}
