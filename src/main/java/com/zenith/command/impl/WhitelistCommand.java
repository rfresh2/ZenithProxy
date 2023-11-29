package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

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
            CommandCategory.CORE,
            "Manage the proxy's whitelist. Only usable by users with the account owner role.",
            asList("add/del <player>", "list", "clear"),
            asList("wl")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("whitelist")
                .then(literal("add").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    WHITELIST_MANAGER.addWhitelistEntryByUsername(player).ifPresentOrElse(e ->
                                    c.getSource().getEmbedBuilder()
                                            .title("Added user: " + escape(e.username) + " To Whitelist"),
                            () -> c.getSource().getEmbedBuilder()
                                    .title("Failed to add user: " + escape(player) + " to whitelist. Unable to lookup profile."));
                    return 1;
                })))
                .then(literal("del").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    WHITELIST_MANAGER.removeWhitelistEntryByUsername(player);
                    c.getSource().getEmbedBuilder()
                            .title("Removed user: " + escape(player) + " From Whitelist");
                    WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist List");
                }))
                .then(literal("clear").requires(Command::validateAccountOwner).executes(c -> {
                    WHITELIST_MANAGER.clearWhitelist();
                    c.getSource().getEmbedBuilder()
                            .title("Whitelist Cleared");
                    WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                    return 1;
                }));
    }

    private String whitelistToString() {
        return CONFIG.server.extra.whitelist.whitelist.isEmpty()
                ? "Empty"
                : CONFIG.server.extra.whitelist.whitelist.stream()
                .map(mp -> escape(mp.username + " [[Link](" + mp.getNameMCLink() + ")]"))
                .collect(Collectors.joining("\n"));
    }

    private String whitelistToStringShort() {
        return CONFIG.server.extra.whitelist.whitelist.isEmpty()
            ? "Empty"
            : CONFIG.server.extra.whitelist.whitelist.stream()
            .map(mp -> escape(mp.username))
            .collect(Collectors.joining("\n"));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            // max chars is 4096. just using size here to approximate randomly. still possible to overflow the description with the "short" version
            .description(CONFIG.server.extra.whitelist.whitelist.size() < 10 ? whitelistToString() : whitelistToStringShort())
            .color(Color.CYAN);
    }
}
