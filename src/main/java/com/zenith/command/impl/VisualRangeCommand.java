package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.CommandOutputHelper.playerListToString;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class VisualRangeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "visualRange",
            CommandCategory.MODULE,
            "Configure the VisualRange notification feature",
            asList(
                        "on/off",
                        "mention on/off",
                        "friend add/del <player>",
                        "friend list",
                        "friend clear",
                        "ignoreFriends on/off",
                        "leave on/off",
                        "logout on/off",
                        "enemyTracking on/off"
                ),
            asList("vr")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("visualRange")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.visualRangeAlert = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("VisualRange " + toggleStrCaps(CONFIG.client.extra.visualRangeAlert));
                return 1;
            }))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeAlertMention = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("VisualRange Mentions " + toggleStrCaps(CONFIG.client.extra.visualRangeAlertMention));
                            return 1;
                      })))
            .then(literal("friend")
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
                      }))
                      .then(literal("clear").executes(c -> {
                          PLAYER_LISTS.getFriendsList().clear();
                          c.getSource().getEmbed()
                              .title("Friend list cleared!");
                      })))
            .then(literal("ignoreFriends")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeIgnoreFriends = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Ignore Friends " + toggleStrCaps(CONFIG.client.extra.visualRangeIgnoreFriends));
                            return 1;
                      })))
            .then(literal("leave")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeLeftAlert = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Leave Alerts " + toggleStrCaps(CONFIG.client.extra.visualRangeLeftAlert));
                            return 1;
                      })))
            .then(literal("logout")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeLeftLogoutAlert = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Logout Alerts " + toggleStrCaps(CONFIG.client.extra.visualRangeLeftLogoutAlert));
                            return 1;
                      })))
            .then(literal("enemyTracking")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangePositionTracking = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Enemy Tracking " + toggleStrCaps(CONFIG.client.extra.visualRangePositionTracking));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .description("**Friend List**\n" + playerListToString(PLAYER_LISTS.getFriendsList()))
            .addField("VisualRange Alerts", toggleStr(CONFIG.client.extra.visualRangeAlert), false)
            .addField("Mentions", toggleStr(CONFIG.client.extra.visualRangeAlertMention), false)
            .addField("Ignore Friends", toggleStr(CONFIG.client.extra.visualRangeIgnoreFriends), false)
            .addField("Leave Alerts", toggleStr(CONFIG.client.extra.visualRangeLeftAlert), false)
            .addField("Logout Alerts", toggleStr(CONFIG.client.extra.visualRangeLeftLogoutAlert), false)
            .addField("Enemy Tracking", toggleStr(CONFIG.client.extra.visualRangePositionTracking), false)
            .color(Color.CYAN);
    }
}
