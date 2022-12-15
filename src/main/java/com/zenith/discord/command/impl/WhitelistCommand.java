package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.WHITELIST_MANAGER;
import static java.util.Arrays.asList;

public class WhitelistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "whitelist",
                "Manage the proxy's whitelist. Only usable by users with the account owner role.",
                asList("add/del <player>", "list", "clear")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("whitelist")
                .then(literal("add").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    if (WHITELIST_MANAGER.addWhitelistEntryByUsername(player)) {
                        c.getSource().getEmbedBuilder()
                                .title("Added user: " + escape(player) + " To Whitelist")
                                .color(Color.CYAN)
                                .addField("Whitelisted", whitelistToString(), false);
                    } else {
                        c.getSource().getEmbedBuilder()
                                .title("Failed to add user: " + escape(player) + " to whitelist. Unable to lookup profile.")
                                .color(Color.RUBY);
                    }
                    return 1;
                })))
                .then(literal("del").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    WHITELIST_MANAGER.removeWhitelistEntryByUsername(player);
                    c.getSource().getEmbedBuilder()
                            .title("Removed user: " + escape(player) + " From Whitelist")
                            .color(Color.CYAN)
                            .addField("Whitelisted", whitelistToString(), false);
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist List")
                            .color(Color.CYAN)
                            .addField("Whitelisted", whitelistToString(), false);
                }))
                .then(literal("clear").requires(Command::validateAccountOwner).executes(c -> {
                    WHITELIST_MANAGER.clearWhitelist();
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist Cleared")
                            .color(Color.RUBY)
                            .addField("Whitelisted", whitelistToString(), false);
                    return 1;
                }));
    }

    private String whitelistToString() {
        return CONFIG.server.extra.whitelist.whitelist.isEmpty()
                ? "Empty"
                : String.join("\n",
                CONFIG.server.extra.whitelist.whitelist.stream()
                        .map(mp -> escape(mp.username + " [" + mp.uuid.toString() + "]"))
                        .collect(Collectors.toList()));
    }
}
