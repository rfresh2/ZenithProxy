package com.zenith.plugin.bootstrap;

import com.zenith.plugin.api.PluginInfo;
import com.zenith.testfixtures.SharedLoaderFixtures;
import com.zenith.testfixtures.SharedLoaderFixtures.*;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.FailStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SharedApplicationClassLoaderTest {
    private static final String TARGET_NAME = TransformTarget.class.getName();

    @TempDir
    Path tempDir;

    @Test
    void applicationAndPluginClassesUseTheSharedLoader() throws Exception {
        var pluginJar = writeJar(tempDir.resolve("plugin.jar"), Map.of(
            classEntry(PluginClass.class), classBytes(PluginClass.class)
        ));
        try (var loader = newLoader(pluginJar)) {
            var applicationClass = loader.loadClass(PluginInfo.class.getName());
            var pluginClass = loader.loadClass(PluginClass.class.getName());

            assertSame(loader, applicationClass.getClassLoader());
            assertSame(loader, pluginClass.getClassLoader());
        }
    }

    @Test
    void serviceLoaderFindsProvidersFromEveryPluginJar() throws Exception {
        var first = writeJar(tempDir.resolve("first.jar"), Map.of(
            classEntry(FirstProvider.class), classBytes(FirstProvider.class),
            "META-INF/services/java.lang.Runnable", (FirstProvider.class.getName() + "\n").getBytes(StandardCharsets.UTF_8)
        ));
        var second = writeJar(tempDir.resolve("second.jar"), Map.of(
            classEntry(SecondProvider.class), classBytes(SecondProvider.class),
            "META-INF/services/java.lang.Runnable", (SecondProvider.class.getName() + "\n").getBytes(StandardCharsets.UTF_8)
        ));
        try (var loader = newLoader(first, second)) {
            var names = new HashSet<String>();
            for (var provider : ServiceLoader.load(Runnable.class, loader)) {
                names.add(provider.getClass().getName());
                assertSame(loader, provider.getClass().getClassLoader());
            }

            assertEquals(Set.of(FirstProvider.class.getName(), SecondProvider.class.getName()), names);
        }
    }

    @Test
    void successfulTransformationDefinesTheTransformedClassInTheSharedLoader() throws Exception {
        var targetJar = writeJar(tempDir.resolve("target.jar"), Map.of(
            classEntry(SharedLoaderFixtures.class), classBytes(SharedLoaderFixtures.class),
            classEntry(PluginClass.class), classBytes(PluginClass.class),
            classEntry(FirstProvider.class), classBytes(FirstProvider.class),
            classEntry(SecondProvider.class), classBytes(SecondProvider.class),
            classEntry(TransformTarget.class), classBytes(TransformTarget.class),
            classEntry(TransformMixin.class), classBytes(TransformMixin.class)
        ));
        try (var loader = newLoader(targetJar)) {
            var manager = new TransformerManager(new GuavaClassPathProvider(loader));
            manager.setFailStrategy(FailStrategy.THROW);
            manager.addTransformerPreprocessor(new MixinsTranslator());
            manager.addTransformer("com.zenith.testfixtures.**");
            loader.configureTransformers(manager, () -> { });

            var target = loader.loadClass(TARGET_NAME);

            assertSame(loader, target.getClassLoader());
            assertEquals("patched!", target.getMethod("value").invoke(null));
        }
    }

    @Test
    void failedTransformationFallsBackToOriginalBytesAndDisablesTransformers() throws Exception {
        var targetJar = writeJar(tempDir.resolve("target-failure.jar"), Map.of(
            classEntry(TransformTarget.class), classBytes(TransformTarget.class)
        ));
        var failures = new AtomicInteger();
        try (var loader = newLoader(targetJar)) {
            var manager = new TransformerManager(new GuavaClassPathProvider(loader));
            manager.setFailStrategy(FailStrategy.THROW);
            manager.addBytecodeTransformer((name, bytes, transitive) -> {
                if (TARGET_NAME.equals(name)) throw new IllegalStateException("test transformer failure");
                return null;
            });
            loader.configureTransformers(manager, failures::incrementAndGet);

            var target = loader.loadClass(TARGET_NAME);

            assertSame(loader, target.getClassLoader());
            assertEquals("original", target.getMethod("value").invoke(null));
            assertEquals(1, failures.get());
            assertSame(target, loader.loadClass(TARGET_NAME));
        }
    }

    private SharedApplicationClassLoader newLoader(Path... pluginJars) throws Exception {
        var urls = new ArrayList<URL>();
        urls.add(PluginInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL());
        for (var pluginJar : pluginJars) urls.add(pluginJar.toUri().toURL());
        return new SharedApplicationClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
    }

    private static String classEntry(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        var resourceName = classEntry(type);
        try (var stream = type.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) throw new IOException("Missing class resource: " + resourceName);
            return stream.readAllBytes();
        }
    }

    private static Path writeJar(Path path, Map<String, byte[]> entries) throws IOException {
        try (var stream = Files.newOutputStream(path); var jar = new JarOutputStream(stream)) {
            for (var entry : entries.entrySet()) {
                jar.putNextEntry(new JarEntry(entry.getKey()));
                jar.write(entry.getValue());
                jar.closeEntry();
            }
        }
        return path;
    }

}
