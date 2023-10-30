package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.WHITELIST_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class VisualRangeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "visualRange",
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
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("visualRange")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.visualRangeAlert = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("VisualRange " + (CONFIG.client.extra.visualRangeAlert ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeAlertMention = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("VisualRange Mentions " + (CONFIG.client.extra.visualRangeAlertMention ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("friend")
                      .then(literal("add").then(argument("player", string()).executes(c -> {
                          final String player = StringArgumentType.getString(c, "player");
                          WHITELIST_MANAGER.addFriendWhitelistEntryByUsername(player)
                              .ifPresentOrElse(e ->
                                                   c.getSource().getEmbedBuilder()
                                                       .title("Friend added"),
                                               () -> c.getSource().getEmbedBuilder()
                                                   .title("Failed to add user: " + escape(player) + " to friends. Unable to lookup profile."));
                          return 1;
                      })))
                      .then(literal("del").then(argument("player", string()).executes(c -> {
                          final String player = StringArgumentType.getString(c, "player");
                          WHITELIST_MANAGER.removeFriendWhitelistEntryByUsername(player);
                          c.getSource().getEmbedBuilder()
                              .title("Friend deleted");
                          return 1;
                      })))
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbedBuilder()
                              .title("Friend list");
                      }))
                      .then(literal("clear").executes(c -> {
                          WHITELIST_MANAGER.clearFriendWhitelist();
                          c.getSource().getEmbedBuilder()
                              .title("Friend list cleared!");
                      })))
            .then(literal("ignorefriends")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeIgnoreFriends = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Ignore Friends " + (CONFIG.client.extra.visualRangeIgnoreFriends ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("leave")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeLeftAlert = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Leave Alerts " + (CONFIG.client.extra.visualRangeLeftAlert ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("logout")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangeLeftLogoutAlert = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Logout Alerts " + (CONFIG.client.extra.visualRangeLeftLogoutAlert ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("enemytracking")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRangePositionTracking = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Enemy Tracking " + (CONFIG.client.extra.visualRangePositionTracking ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public List<String> aliases() {
        return asList("vr");
    }

    private String friendListString() {
        return CONFIG.client.extra.friendsList.isEmpty()
                ? "Empty"
                : CONFIG.client.extra.friendsList.stream()
                .map(e -> escape(e.username + " [[" + e.uuid.toString() + "](" + e.getNameMCLink() + ")]"))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .description("Friend List: \n " + friendListString())
            .addField("VisualRange Alerts", toggleStr(CONFIG.client.extra.visualRangeAlert), false)
            .addField("Mentions", toggleStr(CONFIG.client.extra.visualRangeAlertMention), false)
            .addField("Ignore Friends", toggleStr(CONFIG.client.extra.visualRangeIgnoreFriends), false)
            .addField("Leave Alerts", toggleStr(CONFIG.client.extra.visualRangeLeftAlert), false)
            .addField("Logout Alerts", toggleStr(CONFIG.client.extra.visualRangeLeftLogoutAlert), false)
            .addField("Enemy Tracking", toggleStr(CONFIG.client.extra.visualRangePositionTracking), false)
            .color(Color.CYAN);
    }
}
