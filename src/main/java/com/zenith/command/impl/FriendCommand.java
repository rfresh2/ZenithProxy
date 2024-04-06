package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.util.CommandOutputHelper.playerListToString;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class FriendCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "friend",
            CommandCategory.MANAGE,
            """
            Manage the friend list.
            Friends change behavior for various modules like VisualRange, KillAura, and AutoDisconnect
            """,
            asList(
                "add/del <player>",
                "list",
                "clear"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("friend")
            .then(literal("add").then(argument("player", string()).executes(c -> {
                final String player = StringArgumentType.getString(c, "player");
                PLAYER_LISTS.getFriendsList().add(player)
                    .ifPresentOrElse(e ->
                                         c.getSource().getEmbed()
                                             .title("Friend added"),
                                     () -> c.getSource().getEmbed()
                                         .title("Failed to add user: " + escape(player) + " to friends. Unable to lookup profile."));
                return 1;
            })))
            .then(literal("del").then(argument("player", string()).executes(c -> {
                final String player = StringArgumentType.getString(c, "player");
                PLAYER_LISTS.getFriendsList().remove(player);
                c.getSource().getEmbed()
                    .title("Friend deleted");
                return 1;
            })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Friend list");
                return 1;
            }))
            .then(literal("clear").executes(c -> {
                PLAYER_LISTS.getFriendsList().clear();
                c.getSource().getEmbed()
                    .title("Friend list cleared!");
                return 1;
            }));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .description("**Friend List**\n" + playerListToString(PLAYER_LISTS.getFriendsList()))
            .primaryColor();
    }
}
