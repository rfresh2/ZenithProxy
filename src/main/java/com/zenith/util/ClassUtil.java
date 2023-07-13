package com.zenith.util;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.zenith.Shared.DEFAULT_LOG;

public class ClassUtil {
    // stolen from gs++ because i am lazy
    public static ArrayList<Class<?>> findClassesInPath(String classPath) {
        final ArrayList<Class<?>> foundClasses = new ArrayList<>();
        String resource = ClassUtil.class.getClassLoader().getResource(classPath.replace(".", "/")).getPath();
        if (resource.contains("!")) {
            try (ZipInputStream file = new ZipInputStream(URI.create(resource.substring(0, resource.lastIndexOf('!'))).toURL().openStream())) {
                ZipEntry entry;
                while ((entry = file.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith(classPath.replace(".", "/") + "/") && name.endsWith(".class")) {
                        try {
                            Class<?> clazz = Class.forName(name.substring(0, name.length() - 6).replace("/", "."));
                            foundClasses.add(clazz);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                DEFAULT_LOG.error("Error finding classes in path {}", classPath, e);
            }
        } else {
            try {
                URL classPathURL = ClassUtil.class.getClassLoader().getResource(classPath.replace(".", "/"));
                if (classPathURL != null) {
                    File file = new File(classPathURL.getFile());
                    if (file.exists()) {
                        String[] classNamesFound = file.list();
                        if (classNamesFound != null) {
                            for (String className : classNamesFound) {
                                if (className.endsWith(".class")) {
                                    foundClasses.add(Class.forName(classPath + "." + className.substring(0, className.length() - 6)));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        foundClasses.sort(Comparator.comparing(Class::getName));
        return foundClasses;
    }
}

