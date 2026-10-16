package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class CommandConfigCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("commandConfig")
            .category(CommandCategory.MANAGE)
            .description("""
            Configures ZenithProxy command prefixes and settings.
            """)
            .usageLines(
                "discord prefix <string>",
                "ingame on/off",
                "ingame slashCommands on/off",
                "ingame slashCommands replaceServerCommands on/off",
                "ingame slashCommands suggestions on/off",
                "ingame prefix <string>",
                "ingame allowWhitelistedToUseAccountOwnerCommands on/off"
                // todo: might add command to config these at some point. But I think these should always be on
//                "ingame logToDiscord on/off",
//                "terminal logToDiscord on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("commandConfig").requires(Command::validateAccountOwner)
            .then(literal("discord").then(literal("prefix").then(argument("prefix", wordWithChars())
                .executes(c -> {
                    final String newPrefix = c.getArgument("prefix", String.class);
                    if (newPrefix.isBlank()) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Prefix must be at least one character");
                        return OK;
                    }
                    CONFIG.discord.prefix = newPrefix;
                    c.getSource().getEmbed()
                        .title("Command Config")
                        .description("Set discord prefix to " + CONFIG.discord.prefix);
                    return OK;
                }))))
            .then(literal("ingame")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.inGameCommands.enable = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("In Game Commands " + toggleStrCaps(CONFIG.inGameCommands.enable));
                }))
                .then(literal("slashCommands").then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.inGameCommands.slashCommands = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("In Game Slash Commands " + toggleStrCaps(CONFIG.inGameCommands.slashCommands));
                        syncSlashCommandsToCurrentPlayer();
                    }))
                    .then(literal("replaceServerCommands").then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.inGameCommands.slashCommandsReplacesServerCommands = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("Replace Server Commands " + toggleStrCaps(CONFIG.inGameCommands.slashCommandsReplacesServerCommands));
                        syncSlashCommandsToCurrentPlayer();
                    })))
                    .then(literal("suggestions").then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.inGameCommands.suggestions = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("In Game Slash Commands Suggestions " + toggleStrCaps(CONFIG.inGameCommands.suggestions));
                    }))))
                .then(literal("prefix").then(argument("prefix", wordWithChars()).executes(c -> {
                    final String newPrefix = c.getArgument("prefix", String.class);
                    if (newPrefix.isBlank()) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Prefix must be at least one character");
                        return ERROR;
                    } else {
                        CONFIG.inGameCommands.prefix = newPrefix;
                        c.getSource().getEmbed()
                            .title("Command Config")
                            .description("Set ingame prefix to " + CONFIG.inGameCommands.prefix);
                        return OK;
                    }
                })))
                .then(literal("allowWhitelistedToUseAccountOwnerCommands").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.inGameCommands.allowWhitelistedToUseAccountOwnerCommands = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Allow Whitelisted Use Account Owner Commands " + toggleStrCaps(CONFIG.inGameCommands.allowWhitelistedToUseAccountOwnerCommands));
                }))));
    }

    private static void syncSlashCommandsToCurrentPlayer() {
        var session = Proxy.getInstance().getCurrentPlayer().get();
        if (session != null) {
            CACHE.getChatCache().getPackets(session::sendAsync, session);
        }
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Discord Prefix", CONFIG.discord.prefix)
            .addField("Ingame Commands", toggleStr(CONFIG.inGameCommands.enable))
            .addField("Ingame Slash Commands", toggleStr(CONFIG.inGameCommands.slashCommands))
            .addField("Ingame Slash Commands Replace Server Commands", toggleStr(CONFIG.inGameCommands.slashCommandsReplacesServerCommands))
            .addField("Ingame Slash Command Suggestions", toggleStr(CONFIG.inGameCommands.suggestions))
            .addField("Ingame Prefix", CONFIG.inGameCommands.prefix)
            .addField("Ingame Allow Whitelisted To Use Account Owner Commands", toggleStr(CONFIG.inGameCommands.allowWhitelistedToUseAccountOwnerCommands))
            .primaryColor();
    }
}
