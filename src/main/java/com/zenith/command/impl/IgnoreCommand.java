package com.zenith.command.impl;

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

public class IgnoreCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("ignore",
                                 CommandCategory.MODULE,
                                 "Ignores a player",
                                 asList(
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
                    PLAYER_LISTS.getIgnoreList().add(player).ifPresentOrElse(
                            ignored -> c.getSource().getEmbed()
                                    .title(escape(ignored.getUsername()) + " ignored!"),
                            () -> c.getSource().getEmbed()
                                    .title("Failed to add " + escape(player) + " to ignore list. Unable to lookup profile.")
                                    .errorColor());
                    return 1;
                })))
                .then(literal("del").then(argument("player", string()).executes(c -> {
                    String player = c.getArgument("player", String.class);
                    PLAYER_LISTS.getIgnoreList().remove(player);
                    c.getSource().getEmbed()
                            .title(escape(player) + " removed from ignore list!");
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbed()
                            .title("Ignore List");
                }))
                .then(literal("clear").executes(c -> {
                    PLAYER_LISTS.getIgnoreList().clear();
                    c.getSource().getEmbed()
                            .title("Ignore list cleared!");
                    return 1;
                }));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .description("**Ignore List**\n" + playerListToString(PLAYER_LISTS.getIgnoreList()))
            .primaryColor();
    }
}
