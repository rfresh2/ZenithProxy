package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.CommandOutputHelper.playerListToString;
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
                    PLAYER_LISTS.getWhitelist().add(player).ifPresentOrElse(e ->
                                    c.getSource().getEmbed()
                                            .title("Added user: " + escape(e.getUsername()) + " To Whitelist"),
                                                                            () -> c.getSource().getEmbed()
                                    .title("Failed to add user: " + escape(player) + " to whitelist. Unable to lookup profile."));
                    return 1;
                })))
                .then(literal("del").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    PLAYER_LISTS.getWhitelist().remove(player);
                    c.getSource().getEmbed()
                            .title("Removed user: " + escape(player) + " From Whitelist");
                    Proxy.getInstance().kickNonWhitelistedPlayers();
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbed()
                            .title("Whitelist List");
                }))
                .then(literal("clear").requires(Command::validateAccountOwner).executes(c -> {
                    PLAYER_LISTS.getWhitelist().clear();
                    c.getSource().getEmbed()
                            .title("Whitelist Cleared");
                    Proxy.getInstance().kickNonWhitelistedPlayers();
                    return 1;
                }));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .description(playerListToString(PLAYER_LISTS.getWhitelist()))
            .color(Color.CYAN);
    }
}
