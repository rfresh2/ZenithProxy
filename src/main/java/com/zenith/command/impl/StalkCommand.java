package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.CommandOutputHelper.playerListEntriesToString;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class StalkCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "stalk",
            CommandCategory.MODULE,
            "Configures the stalk module which sends discord mentions when a player connects",
            asList("on/off", "list", "add/del <player>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("stalk")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.stalk.enabled = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("Stalk " + (CONFIG.client.extra.stalk.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbedBuilder()
                    .title("Stalk List");
            }))
            .then(literal("add").then(argument("player", string()).executes(c -> {
                final String player = StringArgumentType.getString(c, "player");
                PLAYER_LISTS.getStalkList().add(player).ifPresentOrElse(e ->
                    c.getSource().getEmbedBuilder()
                            .title("Added player: " + escape(e.getUsername()) + " To Stalk List"),
                        () -> c.getSource().getEmbedBuilder()
                            .title("Failed to add player: " + escape(player) + " to stalk list. Unable to lookup profile."));
                return 1;
            })))
            .then(literal("del").then(argument("player", string()).executes(c -> {
                final String player = StringArgumentType.getString(c, "player");
                PLAYER_LISTS.getStalkList().remove(player);
                c.getSource().getEmbedBuilder()
                    .title("Removed player: " + escape(player) + " From Stalk List");
                return 1;
            })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("Stalk", toggleStr(CONFIG.client.extra.stalk.enabled), false)
            .description("Stalk List:\n: " + playerListEntriesToString(CONFIG.client.extra.stalk.stalking))
            .color(Color.CYAN);
    }
}
