package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.api.Api;
import com.zenith.plugin.PluginManager;
import com.zenith.plugin.api.PluginInfo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
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
        return command("plugins").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.plugins.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Plugins " + toggleStrCaps(CONFIG.plugins.enabled))
                    .addField("Plugins", toggleStr(CONFIG.plugins.enabled), false)
                    .description("Restart ZenithProxy for changes to take effect: `restart`")
                    .primaryColor();
                return OK;
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
                    .description(plugins.isEmpty() ? "None" : info)
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
                if (!api.download(url)) {
                    c.getSource().getEmbed()
                        .title("Download Failed")
                        .description("More info may be in ZenithProxy logs");
                    return ERROR;
                }
                c.getSource().getEmbed()
                    .title("Jar Downloaded")
                    .description("Restart ZenithProxy to reload plugins: `restart`")
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
                            .description("Changes will take effect on next restart")
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

    private static class PluginDownloadApi extends Api {

        public PluginDownloadApi() {
            super("");
        }

        public boolean download(URL url) {
            HttpRequest request = buildBaseRequest(url.toString())
                .GET()
                .build();
            try (var client = buildHttpClient()) {
                var response = client
                    .send(request, HttpResponse.BodyHandlers.ofFileDownload(PluginManager.PLUGINS_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
                if (response.statusCode() >= 400) {
                    PLUGIN_LOG.error("Failed to download plugin from: {} - {}", url, response.statusCode());
                    return false;
                }
                // verify the jar was written to file
                if (!Files.exists(response.body())) {
                    PLUGIN_LOG.error("Failed to download plugin from: {} - File not written", url);
                    return false;
                }
                return true;
            } catch (Exception e) {
                PLUGIN_LOG.error("Failed to download plugin from: {} - {}", url, e.getMessage());
            }
            return false;
        }
    }
}
