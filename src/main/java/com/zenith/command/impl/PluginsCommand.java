package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.api.Api;
import com.zenith.plugin.PluginManager;
import com.zenith.plugin.api.PluginInfo;
import com.zenith.util.ImageInfo;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class PluginsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("plugins")
            .category(CommandCategory.MANAGE)
            .description("""
             [BETA]

             Configures the ZenithProxy plugin manager.

             Plugins are user-created add-ons that add modules and commands.

             Plugins are only supported on the `java` release channel.
             """)
            .usageLines(
                "on/off",
                "list",
                "download <url>",
                "remove <pluginId>"
            )
            .aliases("plugin")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("plugins")
            .requires(c -> Command.validateAccountOwner(c)
                    // todo: consider blocking discord source by default, overridable by config
                    && Command.validateCommandSource(c, List.of(CommandSources.TERMINAL, CommandSources.DISCORD)))
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.plugins.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Plugins " + toggleStrCaps(CONFIG.plugins.enabled))
                    .addField("Plugins", toggleStr(CONFIG.plugins.enabled), false)
                    .description(appendWarningToDescription("Restart ZenithProxy for changes to take effect: `restart`"))
                    .primaryColor();
            }))
            .then(literal("list").executes(c -> {
                var plugins = PLUGIN_MANAGER.getPluginInfos();
                String info = plugins.stream()
                    .sorted(Comparator.comparing(PluginInfo::id))
                    .map(p -> """
                         **%s**
                         * Version: %s
                         * Description: %s
                         * URL: %s
                         * Author(s): %s
                         * MC: %s
                         """.formatted(
                             p.id(),
                             p.version(),
                             p.description(),
                             p.url(),
                             String.join(", ", p.authors()),
                             String.join(", ", p.mcVersions())
                    ))
                    .map(DiscordBot::escape)
                    .collect(Collectors.joining("\n"));
                c.getSource().getEmbed()
                    .title("Loaded Plugins (" + plugins.size() + ")")
                    .description(appendWarningToDescription(plugins.isEmpty() ? "None" : info))
                    .primaryColor();
            }))
            .then(literal("download").then(argument("url", wordWithChars()).executes(c -> {
                var requestedUrl = getString(c, "url");
                if (!requestedUrl.startsWith("http://") && !requestedUrl.startsWith("https://")) {
                    c.getSource().getEmbed()
                        .title("Invalid URL")
                        .description("The URL must start with `http://` or `https://`");
                    return ERROR;
                }
                if (!requestedUrl.endsWith(".jar")) {
                    c.getSource().getEmbed()
                        .title("Invalid URL")
                        .description("The URL must point to a `.jar` file");
                    return ERROR;
                }
                URL url;
                try {
                    url = URI.create(requestedUrl).toURL();
                } catch (MalformedURLException e) {
                    c.getSource().getEmbed()
                        .title("Invalid URL")
                        .description("Invalid URL: " + e.getClass().getSimpleName() + " : " + e.getMessage());
                    return ERROR;
                }
                var api = new PluginDownloadApi();
                var downloadResult = api.download(url);
                if (!downloadResult.success()) {
                    c.getSource().getEmbed()
                        .title("Download Failed")
                        .description(downloadResult.error());
                    if (downloadResult.file() != null) downloadResult.file().delete();
                    return ERROR;
                }
                var readResult = readPluginInfo(downloadResult.file());
                if (!readResult.success()) {
                    c.getSource().getEmbed()
                        .title("Invalid Plugin Jar")
                        .description(readResult.error());
                    downloadResult.file().delete();
                    return ERROR;
                }
                var pluginId = readResult.pluginInfo().id();
                var existingPlugin = PLUGIN_MANAGER.getPluginInstance(pluginId);
                String desc = "Restart ZenithProxy to reload plugins: `restart`";
                if (existingPlugin != null) {
                    existingPlugin.getJarPath().toFile().deleteOnExit();
                    desc += "\n\nExisting plugin with ID: `%s` found. It will be replaced/updated on next restart.".formatted(pluginId);
                }
                c.getSource().getEmbed()
                    .title("Plugin Downloaded")
                    .description(appendWarningToDescription(desc))
                    .addField("ID", pluginId)
                    .addField("Description", readResult.pluginInfo().description())
                    .addField("Version", readResult.pluginInfo().version())
                    .addField("URL", readResult.pluginInfo().url())
                    .addField("Author(s)", String.join(", ", readResult.pluginInfo().authors()))
                    .addField("Jar", downloadResult.file().toPath().getFileName())
                    .primaryColor();
                return OK;
            })))
            .then(literal("remove").then(argument("pluginId", wordWithChars()).executes(c -> {
                String id = getString(c, "pluginId");
                for (var instance : PLUGIN_MANAGER.getPluginInstances()) {
                    if (instance.getId().equalsIgnoreCase(id)) {
                        instance.getJarPath().toFile().deleteOnExit();
                        c.getSource().getEmbed()
                            .title("Plugin Removed")
                            .description(appendWarningToDescription("Changes will take effect on next restart"))
                            .addField("Plugin", instance.getPluginInfo().id())
                            .addField("Jar", instance.getJarPath().toString())
                            .primaryColor();
                        return OK;
                    }
                }
                c.getSource().getEmbed()
                    .title("Plugin Jar Not Found");
                return ERROR;
            })));
    }

    private String appendWarningToDescription(String description) {
        if (ImageInfo.inImageRuntimeCode() && PLUGIN_MANAGER.getPluginInfos().isEmpty()) {
            return """
                   %s

                   You must switch to the `java` release channel for plugins to work: `channel set java %s`"""
                .formatted(description, Objects.requireNonNullElse(LAUNCH_CONFIG.getMcVersion(), MinecraftCodec.CODEC.getMinecraftVersion()));
        }
        return description;
    }

    private PluginInfoReadResult readPluginInfo(File jarFile) {
        var zipUri = URI.create("jar:file:" + jarFile.toURI().getPath());
        try (var fs = FileSystems.newFileSystem(zipUri, Collections.emptyMap())) {
            var root = fs.getPath("/");
            var pluginJson = root.resolve("zenithproxy.plugin.json");
            if (!Files.exists(pluginJson)) {
                return new PluginInfoReadResult(false, "No zenithproxy.plugin.json found in jar", null);
            }
            // should never be larger than a few kb
            if (Files.size(pluginJson) > 100 * 1024) {
                return new PluginInfoReadResult(false, "zenithproxy.plugin.json is too large", null);
            }
            var jsonString = Files.readString(pluginJson);
            var pluginInfo = OBJECT_MAPPER.readValue(jsonString, PluginInfo.class);
            return new PluginInfoReadResult(true, null, pluginInfo);
        } catch (Exception e) {
            return new PluginInfoReadResult(false, e.getMessage(), null);
        }
    }

    record PluginInfoReadResult(boolean success, @Nullable String error, @Nullable PluginInfo pluginInfo) {}

    private static class PluginDownloadApi extends Api {

        public PluginDownloadApi() {
            super("");
        }

        public PluginDownloadResult download(URL url) {
            HttpRequest request = buildBaseRequest(url.toString())
                .GET()
                .build();
            File resFile = null;
            try (var client = buildHttpClient()) {
                var response = client
                    .send(request, HttpResponse.BodyHandlers.ofFileDownload(PluginManager.PLUGINS_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
                if (response.statusCode() >= 400) {
                    PLUGIN_LOG.error("Failed to download plugin from: {} - {}", url, response.statusCode());
                    return new PluginDownloadResult(false, "Failed to download plugin, HTTP error code: %s".formatted(url, response.statusCode()), null);
                }
                // verify the jar was written to file
                if (!Files.exists(response.body())) {
                    PLUGIN_LOG.error("Failed to download plugin from: {} - File not written", url);
                    return new PluginDownloadResult(false, "Failed to download plugin, file not written", null);
                }
                resFile = response.body().toFile();
                return new PluginDownloadResult(true, null, resFile);
            } catch (Exception e) {
                PLUGIN_LOG.error("Failed to download plugin from: {} - {}", url, e.getMessage());
                return new PluginDownloadResult(false, "Failed to download plugin, %s".formatted(e.getMessage()), resFile);
            }
        }
    }

    record PluginDownloadResult(boolean success, @Nullable String error, @Nullable File file) { }
}
