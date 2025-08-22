package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.zenith.Globals.PLAYER_LISTS;
import static com.zenith.command.api.CommandOutputHelper.playerListToString;
import static com.zenith.discord.DiscordBot.escape;

public class IgnoreCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("ignore")
            .category(CommandCategory.MODULE)
            .description("""
             Hides chat and death messages and notifications from a configured list of players.
             """)
            .usageLines(
                "add/del <player>",
                "addAll <player 1>,<player 2>...",
                "list",
                "clear"
            )
            .build();
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
                return OK;
            })))
            .then(literal("addAll").then(argument("playerList", greedyString()).executes(c -> {
                String playerList = getString(c, "playerList");
                String[] split = playerList.split(",");
                if (split.length == 0) {
                    c.getSource().getEmbed()
                        .title("Invalid Input")
                        .description("Each player name must be delimited by `,`");
                    return ERROR;
                }
                List<String> addErrors = new ArrayList<>();
                for (var player : split) {
                    if (PLAYER_LISTS.getIgnoreList().add(player).isEmpty()) {
                        addErrors.add(player);
                    }
                }
                c.getSource().getEmbed()
                    .title("Added Players")
                    .addField("Added Player Count", split.length - addErrors.size());
                if (!addErrors.isEmpty()) {
                    c.getSource().getEmbed()
                        .description("Failed adding " + addErrors.size() + " players: " + String.join(", ", addErrors));
                }
                return OK;
            })))
            .then(literal("del").then(argument("player", string()).executes(c -> {
                String player = c.getArgument("player", String.class);
                PLAYER_LISTS.getIgnoreList().remove(player);
                c.getSource().getEmbed()
                    .title(escape(player) + " removed from ignore list!");
            })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Ignore List");
            }))
            .then(literal("clear").executes(c -> {
                PLAYER_LISTS.getIgnoreList().clear();
                c.getSource().getEmbed()
                    .title("Ignore list cleared!");
            }));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .description("**Ignore List**\n" + playerListToString(PLAYER_LISTS.getIgnoreList()))
            .primaryColor();
    }
}
