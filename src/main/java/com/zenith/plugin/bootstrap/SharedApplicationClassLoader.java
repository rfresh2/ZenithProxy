package com.zenith.plugin.bootstrap;

import com.zenith.discord.Embed;
import net.lenni0451.classtransform.TransformerManager;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class SharedApplicationClassLoader extends URLClassLoader {
    private TransformerManager transformers;
    private Runnable onTransformFailure;
    private volatile boolean transformationsDisabled;

    public SharedApplicationClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void configureTransformers(TransformerManager transformers, Runnable onTransformFailure) {
        this.transformers = transformers;
        this.onTransformFailure = onTransformFailure;
    }

    public synchronized void disableTransformers(String msg, Throwable ex) {
        if (transformationsDisabled) return;
        transformationsDisabled = true;
        if (onTransformFailure != null) onTransformFailure.run();
        if (Thread.currentThread().getContextClassLoader() instanceof SharedApplicationClassLoader && findLoadedClass("com.zenith.Globals") != null) {
            reportError(msg, ex);
        }
    }

    private void reportError(String msg, Throwable ex) {
        com.zenith.Globals.DISCORD.sendEmbedMessage(Embed.builder()
            .title("Plugin Mixin Failure")
            .description("""
                Plugin transformer failure %s
                %s : %s
                restart after fixing/removing the plugin
                """
                .formatted(msg, ex.getClass().getSimpleName(), ex.getMessage()))
            .errorColor()
        );
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            var loaded = findLoadedClass(name);
            if (loaded == null) {
                // Ask the platform loader, not package prefixes: javax.* also contains application APIs.
                try {
                    loaded = ClassLoader.getPlatformClassLoader().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    if (isBootstrapClass(name)) loaded = getParent().loadClass(name);
                    else loaded = findClass(name);
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    private static boolean isBootstrapClass(String name) {
        return name.equals("com.zenith.ProxyLaunchWrapper")
            || name.startsWith("com.zenith.bootstrap.")
            || name.startsWith("net.lenni0451.classtransform.")
            || name.startsWith("org.objectweb.asm.");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (transformers == null || transformationsDisabled || name.equals("com.zenith.plugin.bootstrap.PluginBootstrap")) {
            return super.findClass(name);
        }
        var resource = findResource(name.replace('.', '/') + ".class");
        if (resource == null) throw new ClassNotFoundException(name);
        try {
            var connection = resource.openConnection();
            byte[] original;
            try (var in = connection.getInputStream()) {
                original = in.readAllBytes();
            }
            byte[] transformed;
            try {
                transformed = transformers.transform(name, original);
            } catch (Throwable failure) {
                disableTransformers("applying to " + name, failure);
                return super.findClass(name);
            }
            if (transformed == null) return super.findClass(name);
            var source = resource;
            Manifest manifest = null;
            Certificate[] certificates = null;
            if (connection instanceof JarURLConnection jar) {
                source = jar.getJarFileURL();
                manifest = jar.getManifest();
                certificates = jar.getCertificates();
            }
            defineClassPackage(name, manifest, source);
            try {
                return defineClass(name, transformed, 0, transformed.length, new CodeSource(source, certificates));
            } catch (LinkageError failure) {
                disableTransformers("defining " + name, failure);
                return super.findClass(name);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    private void defineClassPackage(String name, Manifest manifest, URL source) {
        var separator = name.lastIndexOf('.');
        if (separator < 0) return;
        var packageName = name.substring(0, separator);
        var existing = getDefinedPackage(packageName);
        if (existing == null) {
            if (manifest == null) definePackage(packageName, null, null, null, null, null, null, null);
            else definePackage(packageName, manifest, source);
        } else if (existing.isSealed() ? !existing.isSealed(source) : isSealed(packageName, manifest)) {
            throw new SecurityException("Package sealing violation: " + packageName);
        }
    }

    private static boolean isSealed(String name, Manifest manifest) {
        if (manifest == null) return false;
        var attributes = manifest.getAttributes(name.replace('.', '/') + "/");
        var sealed = attributes == null ? null : attributes.getValue(Attributes.Name.SEALED);
        if (sealed == null) sealed = manifest.getMainAttributes().getValue(Attributes.Name.SEALED);
        return "true".equalsIgnoreCase(sealed);
    }

    @Override
    public URL getResource(String name) {
        var own = findResource(name);
        return own != null ? own : ClassLoader.getPlatformClassLoader().getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        var resources = new LinkedHashSet<URL>();
        resources.addAll(Collections.list(findResources(name)));
        resources.addAll(Collections.list(ClassLoader.getPlatformClassLoader().getResources(name)));
        return Collections.enumeration(resources);
    }
}
