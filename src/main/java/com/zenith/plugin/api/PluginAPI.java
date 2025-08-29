package com.zenith.plugin.api;

import com.zenith.command.api.Command;
import com.zenith.module.api.Module;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public interface PluginAPI {
    /**
     * Initializes and loads a configuration file for your plugin.
     * @param fileName The name of the file to save and load. Example: "example-config" would have its config at: "plugins/config/example-config.json"
     * @param configClass The configuration POJO class that is saved and loaded
     * @return Configuration instance. This instance must be used for access and modifications to the configuration.
     *         If no configuration file exists yet, a new instance will still be created and returned.
     */
    <T> T registerConfig(String fileName, Class<T> configClass);

    /**
     * Initializes and loads a configuration file for your plugin, using a custom serializer.
     *
     * May be useful for serializing types that do not have a default GSON serializer.
     *
     * Or if you want to use a different file format (NBT, YAML, TOML, etc).
     */
    <T> T registerConfig(String fileName, Class<T> configClass, ConfigSerializer serializer);

    /**
     * Registers a {@link Module}.
     * Modules can listen to events, be toggled on and off, and register packet handlers
     */
    void registerModule(Module module);

    /**
     * Registers a {@link Command}.
     * Commands can be executed in the terminal, by players in-game, and in discord.
     */
    void registerCommand(Command command);

    /**
     * Gets a logger configured for the plugin instance.
     */
    ComponentLogger getLogger();

    /**
     * Gets the {@link PluginInfo} for the plugin instance.
     *
     * This is the data encoded in the @Plugin annotation.
     */
    PluginInfo getPluginInfo();
}
