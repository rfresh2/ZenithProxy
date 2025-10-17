package com.zenith.util.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class ReflectionFeature implements Feature {
    static final List<String> prefixes = List.of(
        "com.viaversion.nbt",
        "com.viaversion.viabackwards.api",
        "com.viaversion.viabackwards.protocol",
        "com.viaversion.viarewind.api",
        "com.viaversion.viarewind.protocol",
        "com.viaversion.viaversion.api",
        "com.viaversion.viaversion.protocols",
        "com.zenith.database.dto",
        "com.zenith.event",
        "com.zenith.feature.api",
        "com.zenith.feature.queue.mcping",
        "com.zenith.terminal",
        "com.zenith.util.config",
        "com.zenith.via"
    );

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        try {
            var cl = access.getApplicationClassLoader();
            for (String prefix : prefixes) {
                var classNames = scanClasses(cl, prefix);
                for (var className : classNames) {
                    var clazz = cl.loadClass(className);
                    access.registerAsUsed(clazz);
                    registerReflection(clazz);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void registerReflection(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
        if (clazz.isRecord()) {
            var recordComponents = clazz.getRecordComponents();
            if (recordComponents != null) {
                for (var comp : recordComponents) {
                    var accessor = comp.getAccessor();
                    RuntimeReflection.register(accessor);
                }
            }
        }
    }

    private static Set<String> scanClasses(ClassLoader classLoader, String pkg) throws IOException {
        var classes = new HashSet<String>();
        var packageName = pkg.replace(".", "/");
        var resources = classLoader.getResources(packageName);
        while (resources.hasMoreElements()) {
            var packageURL = resources.nextElement();
            if (!packageURL.getProtocol().equals("jar")) continue;
            var jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
            try (var jf = new JarFile(jarFileName)) {
                var jarEntries = jf.entries();
                while (jarEntries.hasMoreElements()) {
                    var entryName = jarEntries.nextElement().getName();
                    if (entryName.startsWith(packageName) && entryName.endsWith(".class")) {
                        var clazzName = entryName
                            // back to class-loadable format
                            .replace("/", ".")
                            // chop off .class suffix
                            .substring(0, entryName.lastIndexOf('.'));
                        classes.add(clazzName);
                    }
                }
            }
        }
        return classes;
    }
}
