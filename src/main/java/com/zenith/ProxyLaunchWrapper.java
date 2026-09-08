package com.zenith;

import com.google.gson.JsonParser;
import com.zenith.plugin.PluginDiscovery;
import com.zenith.plugin.bootstrap.BootstrapMessages;
import com.zenith.plugin.bootstrap.SharedApplicationClassLoader;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.FailStrategy;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alternative entrypoint that configures plugin mixins and classloader
 * for graalvm native images use com.zenith.Proxy entrypoint
 */
public final class ProxyLaunchWrapper {
    private ProxyLaunchWrapper() {}

    public static void main(String[] args) throws Throwable {
        var parent = ProxyLaunchWrapper.class.getClassLoader();
        List<PluginDiscovery.DiscoveredPlugin> plugins = pluginsEnabled()
            ? PluginDiscovery.discover(Path.of("plugins"), parent, minecraftVersion(parent), (m, e) -> BootstrapMessages.report(m, e, false))
            : Collections.emptyList();
        var urls = new LinkedHashSet<URL>();
        for (var entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(Path.of(entry).toAbsolutePath().toUri().toURL());
        }
        for (var plugin : plugins) {
            if (!plugin.path().equals(Path.of(""))) {
                urls.add(plugin.path().toAbsolutePath().toUri().toURL());
            }
        }
        var disabledIds = ConcurrentHashMap.<String>newKeySet();
        var transformingIds = new LinkedHashSet<String>();
        for (var plugin : plugins) {
            if (!plugin.info().mixins().isEmpty()) {
                transformingIds.add(plugin.info().id());
            }
        }
        var loader = new SharedApplicationClassLoader(urls.toArray(URL[]::new), parent);
        var manager = new TransformerManager(new GuavaClassPathProvider(loader));
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        loader.configureTransformers(manager, () -> disabledIds.addAll(transformingIds));
        for (var plugin : plugins) {
            try {
                for (var mixin : plugin.info().mixins()) {
                    manager.addTransformer(mixin);
                }
            } catch (Throwable failure) {
                loader.disableTransformers("registering transformers for " + plugin.info().id(), failure);
                break;
            }
        }
        var mapper = JsonMapper.builder().build();
        var metadata = new ArrayList<String>();
        var paths = new ArrayList<String>();
        for (var plugin : plugins) {
            metadata.add(mapper.writeValueAsString(plugin.info()));
            paths.add(plugin.path().toString());
        }
        Method main;
        try {
            main = prepareMain(loader, metadata, paths, disabledIds);
        } catch (LinkageError failure) {
            loader.disableTransformers("preparing application entrypoint", failure);
            loader.close();
            loader = new SharedApplicationClassLoader(urls.toArray(URL[]::new), parent);
            main = prepareMain(loader, metadata, paths, disabledIds);
        }
//        manager.getDebugger().loadTransformedClasses();
        try {
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Method prepareMain(
        SharedApplicationClassLoader loader,
        List<String> metadata,
        List<String> paths,
        Set<String> disabledIds
    ) throws ReflectiveOperationException {
        Thread.currentThread().setContextClassLoader(loader);
        var bridge = loader.loadClass("com.zenith.plugin.bootstrap.PluginBootstrap");
        bridge.getMethod("install", String[].class, String[].class, Set.class)
            .invoke(null, metadata.toArray(String[]::new), paths.toArray(String[]::new), disabledIds);
        return loader.loadClass("com.zenith.Proxy").getMethod("main", String[].class);
    }

    private static boolean pluginsEnabled() {
        var config = Path.of(System.getProperty("zenith.config.file", "config.json"));
        if (!Files.exists(config)) return true;
        try (var reader = Files.newBufferedReader(config)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var plugins = root.getAsJsonObject("plugins");
            return plugins == null || !plugins.has("enabled") || plugins.get("enabled").getAsBoolean();
        } catch (Exception e) {
            BootstrapMessages.report("Unable to read plugin enabled status, starting without plugins", e, false);
            return false;
        }
    }

    private static String minecraftVersion(ClassLoader loader) throws Exception {
        try (var in = loader.getResourceAsStream("zenith_mc_version.txt")) {
            if (in == null) throw new IllegalStateException("Missing zenith_mc_version.txt");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
    }
}
