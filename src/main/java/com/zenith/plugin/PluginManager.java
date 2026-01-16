package com.zenith.plugin;

import com.zenith.Globals;
import com.zenith.discord.Embed;
import com.zenith.event.plugin.PluginLoadFailureEvent;
import com.zenith.event.plugin.PluginLoadedEvent;
import com.zenith.plugin.api.ConfigSerializer;
import com.zenith.plugin.api.PluginInfo;
import com.zenith.plugin.api.PluginInstance;
import com.zenith.plugin.api.ZenithProxyPlugin;
import com.zenith.util.ImageInfo;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;
import static com.zenith.util.KotlinUtil.getKotlinObject;
import static com.zenith.util.KotlinUtil.isKotlinObject;
import static java.util.Objects.requireNonNull;

@NullMarked
public class PluginManager {
    public static final Path PLUGINS_PATH = Path.of("plugins");
    protected final Map<String, ConfigInstance> pluginConfigurations = new ConcurrentHashMap<>();
    protected final Map<String, PluginInstance> pluginInstances = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public List<PluginInfo> getPluginInfos() {
        return pluginInstances.values().stream().map(PluginInstance::getPluginInfo).collect(Collectors.toList());
    }

    public List<PluginInstance> getPluginInstances() {
        return new ArrayList<>(pluginInstances.values());
    }

    public @Nullable PluginInstance getPluginInstance(String id) {
        return pluginInstances.get(id);
    }

    public @Nullable ZenithProxyPlugin getPlugin(String id) {
        var instance = pluginInstances.get(id);
        if (instance == null) return null;
        return instance.getPluginInstance();
    }

    public String getId(final ZenithProxyPlugin pluginInstance) {
        return pluginInstances.values().stream()
            .filter(i -> i.getPluginInstance() == pluginInstance)
            .findFirst()
            .map(PluginInstance::getId)
            .orElseThrow(() -> new RuntimeException("Plugin instance " + pluginInstance.getClass().getName() + " not found"));
    }

    public @Nullable PluginInfo getPluginInfo(final ZenithProxyPlugin pluginInstance) {
        var id = getId(pluginInstance);
        var instance = getPluginInstance(id);
        if (instance == null) return null;
        return instance.getPluginInfo();
    }

    public List<ConfigInstance> getAllPluginConfigs() {
        return new ArrayList<>(pluginConfigurations.values());
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public record ConfigInstance(Object instance, Class<?> clazz, File file, ConfigSerializer serializer) { }

    public synchronized void initialize() {
        if (initialized.get()) return;
        ensurePluginsFolderExists();
        preLoadPlugins();
        loadPlugins();
        initialized.set(true);
    }

    private void linuxChannelIncompatibilityWarning() {
        var potentialJars = findPotentialPluginJars();
        int potentialPluginCount = potentialJars.size();
        if (potentialPluginCount > 0) {
            DISCORD.sendEmbedMessage(Embed.builder()
                .title("Potential Plugins Found")
                .description("""
                Plugins are not supported on the `linux` release channel.

                To use plugins, switch to the `java` release channel:

                `channel set java %s`

                Detected %d potential plugin jars in the plugins directory.
                """.formatted(Objects.requireNonNullElse(LAUNCH_CONFIG.getMcVersion(), MinecraftCodec.CODEC.getMinecraftVersion()), potentialPluginCount))
                .errorColor()
            );
        }
    }

    private void ensurePluginsFolderExists() {
        try {
            if (!PLUGINS_PATH.toFile().exists()) {
                PLUGINS_PATH.toFile().mkdirs();
            }
        } catch (Exception e) {
            PLUGIN_LOG.error("Error creating plugins directory", e);
        }
    }

    private void preLoadPlugins() {
        preLoadPluginsFromSystemClasspath();
        if (ImageInfo.inImageRuntimeCode()) {
            linuxChannelIncompatibilityWarning();
            return;
        }
        if (ImageInfo.inAgentRuntime()) return;
        var potentialPlugins = findPotentialPluginJars();
        for (var jar : potentialPlugins) {
            try {
                preLoadPotentialPluginJar(jar);
            } catch (Throwable e) {
                PLUGIN_LOG.error("Error loading plugin jar: {}", jar, e);
            }
        }
    }

    private void loadPlugins() {
        for (var instance : pluginInstances.entrySet()) {
            try {
                loadPlugin(instance.getValue());
            } catch (Throwable e) {
                PLUGIN_LOG.error("Error loading plugin: {} : {}", instance.getKey(), instance.getValue().getJarPath(), e);
            }
        }
    }

    private void preLoadPluginsFromSystemClasspath() {
        try {
            // must be called before plugin jar discovery where classloaders are opened
            var resources = ClassLoader.getSystemClassLoader().getResources("zenithproxy.plugin.json");
            while (resources.hasMoreElements()) {
                var resourceUrl = resources.nextElement();
                try (var in = resourceUrl.openStream()) {
                    var pluginInfo = readPluginInfo(in);
                    preLoadPluginInstance(pluginInfo, Path.of(""), ClassLoader.getSystemClassLoader());
                } catch (Exception e) {
                    PLUGIN_LOG.error("Error loading classpath plugin: {}", resourceUrl.toString(), e);
                }
            }
        } catch (Throwable e) {
            PLUGIN_LOG.error("Error loading classpath plugins", e);
        }
    }

    private List<Path> findPotentialPluginJars() {
        if (!PLUGINS_PATH.toFile().exists()) return Collections.emptyList();
        final List<Path> list = new ArrayList<>();
        try (var jarStream = Files.newDirectoryStream(PLUGINS_PATH, p -> p.toFile().isFile() && p.toString().endsWith(".jar"))) {
            for (var jarPath : jarStream) {
                list.add(jarPath);
            }
        } catch (Throwable e) {
            PLUGIN_LOG.error("Error loading plugins", e);
        }
        // sort alphabetically by filename
        list.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return list;
    }

    private void preLoadPotentialPluginJar(final Path jarPath) {
        String id = null;
        URLClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());
            PluginInfo pluginInfo = readPluginInfo(classLoader, jarPath);
            id = requireNonNull(pluginInfo.id(), "Plugin id is null");
            preLoadPluginInstance(pluginInfo, jarPath, classLoader);
        } catch (Throwable e) {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException ignored) { }
            }
            PLUGIN_LOG.error("Error loading plugin: {}", jarPath, e);
            EVENT_BUS.postAsync(new PluginLoadFailureEvent(id, jarPath, e));
        }
    }

    protected void preLoadPluginInstance(final PluginInfo pluginInfo, Path jarPath, ClassLoader classLoader) {
        String id = pluginInfo.id();
        if (pluginInfo.mcVersions().isEmpty()) {
            PLUGIN_LOG.error("Plugin: {} has no MC versions specified", jarPath);
            throw new RuntimeException("Plugin has no MC versions specified");
        }
        if (!pluginInfo.mcVersions().contains("*") && !pluginInfo.mcVersions().contains(MC_VERSION)) {
            PLUGIN_LOG.warn("Plugin: {} not compatible with current MC version. Actual: {}, Plugin Required: {}", jarPath, MC_VERSION, pluginInfo.mcVersions());
            return;
        }

        if (pluginInstances.containsKey(id)) {
            PLUGIN_LOG.info("Found duplicate plugin IDs: {}", id);
            var existing = pluginInstances.get(id);
            if (existing.getPluginInfo().version().compareTo(pluginInfo.version()) < 0) {
                PLUGIN_LOG.info("Unloading existing plugin ID: {} with lower version: {} vs {}", id, existing.getPluginInfo().version(), pluginInfo.version());
                var existingClassloader = existing.getClassLoader();
                if (existingClassloader instanceof URLClassLoader urlClassLoader) {
                    try {
                        urlClassLoader.close();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to close existing plugin classloader", e);
                    }
                }
                pluginInstances.remove(id);
            } else {
                throw new RuntimeException("Plugin id already exists: " + id);
            }
        }

        PLUGIN_LOG.info(
            "Found Plugin:\n  id: {}\n  version: {}\n  description: {}\n  url: {}\n  authors: {}\n  jar: {}",
            pluginInfo.id(),
            pluginInfo.version(),
            pluginInfo.description(),
            pluginInfo.url(),
            pluginInfo.authors(),
            jarPath.getFileName()
        );
        pluginInstances.put(id, new PluginInstance(id, jarPath, pluginInfo, classLoader));
    }

    protected void loadPlugin(final PluginInstance pluginInstance) {
        try {
            var pluginInfo = pluginInstance.getPluginInfo();
            var classLoader = pluginInstance.getClassLoader();
            var jarPath = pluginInstance.getJarPath();
            String entrypoint = requireNonNull(pluginInfo.entrypoint(), "Plugin entrypoint is null");

            PLUGIN_LOG.info("Loading Plugin: {}", pluginInfo.id());

            Class<?> pluginClass = classLoader.loadClass(entrypoint);
            if (!ZenithProxyPlugin.class.isAssignableFrom(pluginClass)) {
                throw new RuntimeException("Plugin does not implement ZenithProxyPlugin interface");
            }
            ZenithProxyPlugin plugin;

            if (isKotlinObject(pluginClass)) {
                plugin = (ZenithProxyPlugin) getKotlinObject(pluginClass);
            } else {
                plugin = (ZenithProxyPlugin) pluginClass.getDeclaredConstructor().newInstance();
            }

            pluginInstance.setPluginInstance(plugin);

            try {
                plugin.onLoad(new InstancedPluginAPI(plugin, pluginInfo));
            } catch (final Throwable e) {
                PLUGIN_LOG.error("Exception in plugin onLoad: {}", jarPath, e);
                pluginInstances.remove(pluginInstance.getId());
                throw new RuntimeException("Exception in plugin onLoad: " + e.getMessage(), e);
            }
            EVENT_BUS.postAsync(new PluginLoadedEvent(pluginInfo));
        } catch (Throwable e) {
            try {
                var classloader = pluginInstance.getClassLoader();
                if (classloader instanceof URLClassLoader urlClassLoader) {
                    urlClassLoader.close();
                }
            } catch (IOException ignored) { }
            PLUGIN_LOG.error("Error loading plugin: {}", pluginInstance, e);
            EVENT_BUS.postAsync(new PluginLoadFailureEvent(pluginInstance.getId(), pluginInstance.getJarPath(), e));
        }
    }

    @SneakyThrows
    private PluginInfo readPluginInfo(URLClassLoader classLoader, Path path) {
        try {
            return readPluginInfo(classLoader, "zenithproxy.plugin.json");
        } catch (Throwable e) {
            if (e.getMessage().contains("not found in jar")) {
                // fall through
            } else {
                PLUGIN_LOG.error("Error reading zenithproxy.plugin.json: {}", path, e);
                throw e;
            }
        }
        try {
            var plugin = readPluginInfo(classLoader, "plugin.json");
            PLUGIN_LOG.warn("{} using deprecated plugin.json. Rebuild to migrate to zenithproxy.plugin.json", path);
            return plugin;
        } catch (Throwable e) {
            if (e.getMessage().endsWith("not found in jar")) {
                // fall through
            } else {
                PLUGIN_LOG.error("Error reading plugin.json: {}", path, e);
                throw e;
            }
        }
        throw new RuntimeException("No zenithproxy.plugin.json found in: " + path);
    }

    @SneakyThrows
    private PluginInfo readPluginInfo(ClassLoader classLoader, String pluginJsonFileName) {
        try (var stream = classLoader.getResourceAsStream(pluginJsonFileName)) {
            if (stream == null) {
                throw new RuntimeException(pluginJsonFileName + " not found in jar");
            }
            return readPluginInfo(stream);
        }
    }

    @SneakyThrows
    private PluginInfo readPluginInfo(InputStream stream) {
        var info = OBJECT_MAPPER.readValue(stream, PluginInfo.class);
        requireNonNull(info.entrypoint(), "Entrypoint is null");
        if (info.entrypoint().isBlank()) throw new RuntimeException("Invalid entrypoint");
        requireNonNull(info.id(), "Plugin id is null");
        if (info.id().isBlank()) throw new RuntimeException("Invalid plugin id");
        if (!PluginInfo.ID_PATTERN.matcher(info.id()).matches()) {
            throw new RuntimeException("Invalid plugin id: " + info.id());
        }
        requireNonNull(info.version(), "Plugin version is null");
        requireNonNull(info.description(), "Plugin description is null");
        requireNonNull(info.url(), "Plugin url is null");
        requireNonNull(info.authors(), "Plugin authors is null");
        requireNonNull(info.mcVersions(), "Plugin mcVersions is null");
        return info;
    }

    public synchronized <T> T registerConfig(String fileName, Class<T> clazz, ConfigSerializer serializer) {
        if (pluginConfigurations.containsKey(fileName)) {
            throw new RuntimeException("Config already registered: " + fileName);
        }
        var config = loadPluginConfig(fileName, clazz, serializer);
        File configFile = resolveConfigFile(fileName, serializer.fileExtension());
        if (!configFile.exists()) {
            if (!configFile.getParentFile().mkdirs() && !configFile.getParentFile().exists()) {
                throw new RuntimeException("Unable to create plugin config directory: " + configFile.getParentFile());
            }
        }
        var configInstance = new ConfigInstance(
            config,
            clazz,
            configFile,
            serializer
        );
        pluginConfigurations.put(fileName, configInstance);
        return config;
    }

    @FunctionalInterface
    public interface ConfigSaver {
        void saveConfig(File file, Object config, ConfigSerializer configSerializer);
    }

    public void saveConfigs(ConfigSaver saver) {
        for (var config : pluginConfigurations.values()) {
            saver.saveConfig(config.file(), config.instance(), config.serializer());
        }
    }

    @SneakyThrows
    private <T> T loadPluginConfig(String fileName, Class<T> clazz, ConfigSerializer serializer) {
        PLUGIN_LOG.debug("Loading plugin config: {}", fileName);
        File configFile = resolveConfigFile(fileName, serializer.fileExtension());
        return Globals.loadConfig(configFile, clazz, serializer);
    }

    private File resolveConfigFile(String fileName, String fileExtension) {
        return PLUGINS_PATH.resolve("config").resolve(fileName + "." + fileExtension).toFile();
    }
}
