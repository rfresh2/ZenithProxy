package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.WHITELIST_MANAGER;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class WhitelistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "whitelist",
                "Manage the proxy's whitelist. Only usable by users with the account owner role.",
                asList("add/del <player>", "list", "clear"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("whitelist")
                .then(literal("add").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    WHITELIST_MANAGER.addWhitelistEntryByUsername(player).ifPresentOrElse(e ->
                                    c.getSource().getEmbedBuilder()
                                            .title("Added user: " + escape(e.username) + " To Whitelist")
                                            .color(Color.CYAN)
                                            .description(whitelistToString()),
                            () -> c.getSource().getEmbedBuilder()
                                    .title("Failed to add user: " + escape(player) + " to whitelist. Unable to lookup profile.")
                                    .color(Color.RUBY));
                    return 1;
                })))
                .then(literal("del").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    WHITELIST_MANAGER.removeWhitelistEntryByUsername(player);
                    c.getSource().getEmbedBuilder()
                            .title("Removed user: " + escape(player) + " From Whitelist")
                            .color(Color.CYAN)
                            .description(whitelistToString());
                    WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist List")
                            .color(Color.CYAN)
                            .description(whitelistToString());
                }))
                .then(literal("clear").requires(Command::validateAccountOwner).executes(c -> {
                    WHITELIST_MANAGER.clearWhitelist();
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist Cleared")
                            .color(Color.RUBY)
                            .description(whitelistToString());
                    WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                    return 1;
                }));
    }

    private String whitelistToString() {
        return CONFIG.server.extra.whitelist.whitelist.isEmpty()
                ? "Empty"
                : CONFIG.server.extra.whitelist.whitelist.stream()
                .map(mp -> escape(mp.username + " [[" + mp.uuid.toString() + "](" + mp.getNameMCLink() + ")]"))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public List<String> aliases() {
        return asList("wl");
    }
}
