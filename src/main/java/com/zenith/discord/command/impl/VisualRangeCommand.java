package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.WHITELIST_MANAGER;
import static java.util.Arrays.asList;

public class VisualRangeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "visualRange",
                "Configure the VisualRange notification feature",
                asList("on/off", "mention on/off", "friend add/del <player>", "friend list", "friend clear"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("visualRange")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.visualRangeAlert = true;
                    c.getSource().getEmbedBuilder()
                            .title("VisualRange On!")
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.visualRangeAlert = false;
                    c.getSource().getEmbedBuilder()
                            .title("VisualRange Off!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("mention")
                                .then(literal("on").executes(c -> {
                                    CONFIG.client.extra.visualRangeAlertMention = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("VisualRange Mentions On!")
                                            .addField("Friend List", friendListString(), false)
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.client.extra.visualRangeAlertMention = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("VisualRange Mentions Off!")
                                            .color(Color.CYAN);
                                })))
                        .then(literal("friend")
                                .then(literal("add").then(argument("player", string()).executes(c -> {
                                    final String player = StringArgumentType.getString(c, "player");
                                    WHITELIST_MANAGER.addFriendWhitelistEntryByUsername(player).ifPresentOrElse(e ->
                                                    c.getSource().getEmbedBuilder()
                                                            .title("Friend added")
                                                            .addField("Friend List", friendListString(), false)
                                                            .color(Color.CYAN),
                                            () -> c.getSource().getEmbedBuilder()
                                                    .title("Failed to add user: " + escape(player) + " to friends. Unable to lookup profile.")
                                                    .addField("Friend List", friendListString(), false)
                                                    .color(Color.CYAN));
                                    return 1;
                                })))
                                .then(literal("del").then(argument("player", string()).executes(c -> {
                                    final String player = StringArgumentType.getString(c, "player");
                                    WHITELIST_MANAGER.removeFriendWhitelistEntryByUsername(player);
                                    c.getSource().getEmbedBuilder()
                                            .title("Friend deleted")
                                            .addField("Friend List", friendListString(), false)
                                            .color(Color.CYAN);
                                    return 1;
                                })))
                                .then(literal("list").executes(c -> {
                                    c.getSource().getEmbedBuilder()
                                            .title("Friend list")
                                            .addField("Friend List", friendListString(), false)
                                            .color(Color.CYAN);
                                }))
                                .then(literal("clear").executes(c -> {
                                    WHITELIST_MANAGER.clearFriendWhitelist();
                                    c.getSource().getEmbedBuilder()
                                            .title("Friend list cleared!")
                                            .color(Color.CYAN);
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
}
