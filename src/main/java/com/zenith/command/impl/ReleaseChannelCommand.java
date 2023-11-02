package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.Shared.LAUNCH_CONFIG;
import static com.zenith.Shared.saveLaunchConfig;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class ReleaseChannelCommand extends Command {
    private static final List<String> PLATFORMS = asList("java", "linux");
    private static final List<String> MINECRAFT_VERSIONS = asList("1.12.2", "1.20.1");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("channel",
                                 CommandCategory.MANAGE,
                                 "Changes the current release channel.",
                                 asList(
                                     "list",
                                     "set <platform> <minecraft version>"
                                 )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("channel")
            .then(literal("list").executes(c -> {
                c.getSource().getEmbedBuilder()
                    .title("Release Channel Info")
                    .addField("Current Release Channel", LAUNCH_CONFIG.release_channel, true)
                    .addField("Available Platforms", PLATFORMS.stream().collect(Collectors.joining("`, `", "`", "`")), false)
                    .addField("Available Minecraft Versions", MINECRAFT_VERSIONS.stream().collect(Collectors.joining("`, `", "`", "`")), false)
                    .color(Color.CYAN);
            }))
            .then(literal("set")
                      .then(argument("channel", wordWithChars())
                                .then(argument("minecraft_version", wordWithChars()).executes(c -> {
                                    final String channel = StringArgumentType.getString(c, "channel");
                                    final String minecraft_version = StringArgumentType.getString(c, "minecraft_version");
                                    if (!PLATFORMS.contains(channel)) {
                                        c.getSource().getEmbedBuilder()
                                            .title("Invalid Platform!")
                                            .description("Available platforms: " + PLATFORMS)
                                            .color(Color.RED);
                                        return 0;
                                    }
                                    if (!MINECRAFT_VERSIONS.contains(minecraft_version)) {
                                        c.getSource().getEmbedBuilder()
                                            .title("Invalid Minecraft Version!")
                                            .description("Available versions: " + MINECRAFT_VERSIONS)
                                            .color(Color.RED);
                                        return 0;
                                    }
                                    LAUNCH_CONFIG.release_channel = channel + "." + minecraft_version;
                                    c.getSource().getEmbedBuilder()
                                        .title("Release Channel Updated!")
                                        .addField("Release Channel", LAUNCH_CONFIG.release_channel, false)
                                        .addField("Info", "Please restart ZenithProxy for changes to take effect.\nYou can use the `update` command to restart.", false)
                                        .color(Color.CYAN);
                                    saveLaunchConfig();
                                    return 1;
                                }))));
    }
}
