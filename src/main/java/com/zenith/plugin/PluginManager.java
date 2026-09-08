package com.zenith.plugin;

import com.zenith.Globals;
import com.zenith.discord.Embed;
import com.zenith.event.plugin.PluginLoadFailureEvent;
import com.zenith.event.plugin.PluginLoadedEvent;
import com.zenith.plugin.api.ConfigSerializer;
import com.zenith.plugin.api.PluginInfo;
import com.zenith.plugin.api.PluginInstance;
import com.zenith.plugin.api.ZenithProxyPlugin;
import com.zenith.plugin.bootstrap.PluginBootstrap;
import com.zenith.util.ImageInfo;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
    private final List<String> pluginLoadOrder = new ArrayList<>();
    private final Set<String> legacyDisabledIds = ConcurrentHashMap.newKeySet();

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
        if (isPluginDisabled(id)) return null;
        var instance = pluginInstances.get(id);
        if (instance == null) return null;
        return instance.getPluginInstance();
    }

    public boolean isPluginDisabled(String id) {
        return PluginBootstrap.disabledIds.contains(id) || legacyDisabledIds.contains(id);
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

        try {
            ensurePluginsFolderExists();
            if (ImageInfo.inImageCode()) {
                preLoadLegacyPlugins();
                if (ImageInfo.inImageRuntimeCode()) {
                    linuxChannelIncompatibilityWarning();
                }
            } else {
                var discoveredPlugins = PluginBootstrap.discoveredPlugins;
                if (discoveredPlugins != null) {
                    preLoadPlugins(discoveredPlugins);
                } else {
                    preLoadLegacyPlugins();
                }
            }
            loadPlugins();
        } finally {
            initialized.set(true);
        }
    }

    private void linuxChannelIncompatibilityWarning() {
        int potentialPluginCount = countPotentialPluginJars();
        if (potentialPluginCount > 0) {
            DISCORD.sendEmbedMessage(Embed.builder()
                .title("Potential Plugins Found")
                .description("""
                External plugin JARs are not supported on the `linux` release channel.

                To use plugins, switch to the `java` release channel:

                `channel set java %s`

                Detected %d potential plugin jars in the plugins directory.
                """.formatted(Objects.requireNonNullElse(LAUNCH_CONFIG.getMcVersion(), MinecraftCodec.CODEC.getMinecraftVersion()), potentialPluginCount))
                .errorColor()
            );
        }
    }

    private int countPotentialPluginJars() {
        if (!Files.isDirectory(PLUGINS_PATH)) return 0;
        var count = 0;
        try (var paths = Files.newDirectoryStream(
            PLUGINS_PATH,
            path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")
        )) {
            for (var ignored : paths) {
                count++;
            }
        } catch (Throwable e) {
            PLUGIN_LOG.error("Error scanning plugins directory", e);
        }
        return count;
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

    private void preLoadPlugins(List<PluginDiscovery.DiscoveredPlugin> descriptors) {
        var classLoader = getClass().getClassLoader();
        for (var descriptor : descriptors) {
            var pluginInfo = descriptor.info();
            try {
                preLoadPluginInstance(pluginInfo, descriptor.path(), classLoader);
            } catch (Throwable e) {
                reportPreloadFailure(pluginInfo.id(), descriptor.path(), e);
            }
        }
    }

    private void preLoadLegacyPlugins() {
        var definingLoader = getClass().getClassLoader();
        var descriptors = ImageInfo.inImageCode()
            ? PluginDiscovery.discoverClasspath(definingLoader, MC_VERSION, PLUGIN_LOG::error)
            : PluginDiscovery.discover(PLUGINS_PATH, definingLoader, MC_VERSION, PLUGIN_LOG::error);
        for (var descriptor : descriptors) {
            var pluginInfo = descriptor.info();
            var jarPath = descriptor.path();
            if (!pluginInfo.mixins().isEmpty()) {
                PLUGIN_LOG.warn("Plugin {} declares transformers but ZenithProxy was launched without ClassTransform", pluginInfo.id());
                legacyDisabledIds.add(pluginInfo.id());
                try {
                    preLoadPluginInstance(pluginInfo, jarPath, definingLoader);
                } catch (Throwable e) {
                    reportPreloadFailure(pluginInfo.id(), jarPath, e);
                }
                continue;
            }

            ClassLoader classLoader = definingLoader;
            try {
                if (!ImageInfo.inImageCode() && !isClasspathPluginPath(jarPath)) {
                    classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, definingLoader);
                }
                preLoadPluginInstance(pluginInfo, jarPath, classLoader);
            } catch (Throwable e) {
                closeLegacyClassLoader(classLoader);
                reportPreloadFailure(pluginInfo.id(), jarPath, e);
            }
        }
    }

    private boolean isClasspathPluginPath(Path path) {
        return path.toString().isEmpty();
    }

    private void reportPreloadFailure(String id, Path jarPath, Throwable failure) {
        PLUGIN_LOG.error("Error loading plugin: {}", jarPath, failure);
        EVENT_BUS.postAsync(new PluginLoadFailureEvent(id, jarPath, failure));
    }

    private void loadPlugins() {
        for (var id : pluginLoadOrder) {
            var instance = pluginInstances.get(id);
            if (instance == null) continue;
            try {
                loadPlugin(instance);
            } catch (Throwable e) {
                PLUGIN_LOG.error("Error loading plugin: {} : {}", id, instance.getJarPath(), e);
            }
        }
    }

    protected void preLoadPluginInstance(final PluginInfo pluginInfo, Path jarPath, ClassLoader classLoader) {
        Objects.requireNonNull(pluginInfo, "pluginInfo");
        Objects.requireNonNull(jarPath, "jarPath");
        Objects.requireNonNull(classLoader, "classLoader");
        String id = requireNonNull(pluginInfo.id(), "Plugin id is null");

        PLUGIN_LOG.info(
            "Found Plugin:\n  id: {}\n  version: {}\n  description: {}\n  url: {}\n  authors: {}\n  jar: {}",
            pluginInfo.id(),
            pluginInfo.version(),
            pluginInfo.description(),
            pluginInfo.url(),
            pluginInfo.authors(),
            jarPath.getFileName()
        );
        if (!pluginInstances.containsKey(id)) {
            pluginLoadOrder.add(id);
        }
        pluginInstances.put(id, new PluginInstance(id, jarPath, pluginInfo, classLoader));
    }

    protected void loadPlugin(final PluginInstance pluginInstance) {
        var id = pluginInstance.getId();
        if (isPluginDisabled(id)) return;
        try {
            var pluginInfo = pluginInstance.getPluginInfo();
            var classLoader = pluginInstance.getClassLoader();
            var jarPath = pluginInstance.getJarPath();
            String entrypoint = requireNonNull(pluginInfo.entrypoint(), "Plugin entrypoint is null");

            PLUGIN_LOG.info("Loading Plugin: {}", pluginInfo.id());

            Class<?> pluginClass = classLoader.loadClass(entrypoint);
            if (isPluginDisabled(id)) return;
            if (!ZenithProxyPlugin.class.isAssignableFrom(pluginClass)) {
                throw new RuntimeException("Plugin does not implement ZenithProxyPlugin interface");
            }
            ZenithProxyPlugin plugin;

            if (isKotlinObject(pluginClass)) {
                plugin = (ZenithProxyPlugin) getKotlinObject(pluginClass);
            } else {
                plugin = (ZenithProxyPlugin) pluginClass.getDeclaredConstructor().newInstance();
            }

            if (isPluginDisabled(id)) return;
            pluginInstance.setPluginInstance(plugin);

            if (isPluginDisabled(id)) return;
            try {
                plugin.onLoad(new InstancedPluginAPI(plugin, pluginInfo));
            } catch (final Throwable e) {
                PLUGIN_LOG.error("Exception in plugin onLoad: {}", jarPath, e);
                if (!isPluginDisabled(id)) pluginInstances.remove(id, pluginInstance);
                throw new RuntimeException("Exception in plugin onLoad: " + e.getMessage(), e);
            }
            if (isPluginDisabled(id)) return;
            EVENT_BUS.postAsync(new PluginLoadedEvent(pluginInfo));
        } catch (Throwable e) {
            closeLegacyClassLoader(pluginInstance.getClassLoader());
            PLUGIN_LOG.error("Error loading plugin: {}", pluginInstance, e);
            EVENT_BUS.postAsync(new PluginLoadFailureEvent(pluginInstance.getId(), pluginInstance.getJarPath(), e));
        }
    }

    private void closeLegacyClassLoader(ClassLoader classLoader) {
        // The shared application loader owns application classes and remains open for background
        // work. Only legacy per-JAR URL loaders may be closed after a failed load.
        if (classLoader == null || classLoader == getClass().getClassLoader()) return;
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            try {
                urlClassLoader.close();
            } catch (IOException ignored) { }
        }
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
