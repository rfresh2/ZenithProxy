package com.zenith.plugin.api;

import lombok.Data;

import java.nio.file.Path;

@Data
public class PluginInstance {
    private final String id;
    private final Path jarPath;
    private final PluginInfo pluginInfo;
    private final ClassLoader classLoader;
    // null before loading
    private ZenithProxyPlugin pluginInstance;
}
