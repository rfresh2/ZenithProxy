package graalvm.com.zenith;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Recursively register graalvm native image reflection for all classes under a package prefix
 */
public abstract class AbstractGraalVMReflectionFeature implements Feature {
    public final List<String> prefixes;

    public AbstractGraalVMReflectionFeature(final List<String> prefixes) {
        this.prefixes = prefixes;
    }

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

    public void registerReflection(Class<?> clazz) {
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

    public Set<String> scanClasses(ClassLoader classLoader, String pkg) throws IOException {
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
