package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.WHITELIST_MANAGER;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class IgnoreCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("ignore", "Ignores a player", asList(
                "add/del <player>",
                "list",
                "clear"
        ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("ignore")
                .then(literal("add").then(argument("player", string()).executes(c -> {
                    String player = c.getArgument("player", String.class);
                    WHITELIST_MANAGER.addIgnoreWhitelistEntryByUsername(player).ifPresentOrElse(
                            ignored -> c.getSource().getEmbedBuilder()
                                    .title(escape(ignored.username) + " ignored!")
                                    .color(Color.CYAN)
                                    .description(ignoreListToString()),
                            () -> c.getSource().getEmbedBuilder()
                                    .title("Failed to add " + escape(player) + " to ignore list. Unable to lookup profile.")
                                    .color(Color.RUBY));
                    return 1;
                })))
                .then(literal("del").then(argument("player", string()).executes(c -> {
                    String player = c.getArgument("player", String.class);
                    WHITELIST_MANAGER.removeIgnoreWhitelistEntryByUsername(player);
                    c.getSource().getEmbedBuilder()
                            .title(escape(player) + " removed from ignore list!")
                            .color(Color.CYAN)
                            .description(ignoreListToString());
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Ignore List")
                            .color(Color.CYAN)
                            .description(ignoreListToString());
                }))
                .then(literal("clear").executes(c -> {
                    WHITELIST_MANAGER.clearIgnoreWhitelist();
                    c.getSource().getEmbedBuilder()
                            .title("Ignore list cleared!")
                            .color(Color.CYAN)
                            .description(ignoreListToString());
                    return 1;
                }));
    }

    private String ignoreListToString() {
        return CONFIG.client.extra.chat.ignoreList.isEmpty()
                ? "Empty"
                : CONFIG.client.extra.chat.ignoreList.stream()
                .map(mp -> escape(mp.username + " [[" + mp.uuid.toString() + "](" + mp.getNameMCLink() + ")]"))
                .collect(Collectors.joining("\n"));
    }
}
