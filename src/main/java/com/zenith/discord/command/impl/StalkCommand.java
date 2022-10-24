package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class StalkCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "stalk",
                "Configures the stalk module which sends discord mentions when a player connects",
                asList("on/off", "list", "add/del <player>")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("stalk")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.stalk.enabled = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Stalk On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.stalk.enabled = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Stalk Off!")
                                    .color(Color.RUBY);
                        }))
                        .then(literal("list").executes(c -> {
                            c.getSource().getEmbedBuilder()
                                    .title("Stalk List")
                                    .color(Color.CYAN)
                                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                                    ? escape(String.join(", ", CONFIG.client.extra.stalk.stalkList))
                                                    : "Stalk list is empty"),
                                            false);
                        }))
                        .then(literal("add").then(argument("player", string()).executes(c -> {
                            final String player = StringArgumentType.getString(c, "player");
                            if (!CONFIG.client.extra.stalk.stalkList.contains(player)) {
                                CONFIG.client.extra.stalk.stalkList.add(player);
                            }
                            c.getSource().getEmbedBuilder()
                                    .title("Added player: " + escape(player) + " To Stalk List")
                                    .color(Color.CYAN)
                                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                                    ? escape(String.join(", ", CONFIG.client.extra.stalk.stalkList))
                                                    : "Stalk list is empty"),
                                            false);
                            return 1;
                        })))
                        .then(literal("del").then(argument("player", string()).executes(c -> {
                            final String player = StringArgumentType.getString(c, "player");
                            CONFIG.client.extra.stalk.stalkList.removeIf(s -> s.equalsIgnoreCase(player));
                            c.getSource().getEmbedBuilder()
                                    .title("Removed player: " + escape(player) + " From Stalk List")
                                    .color(Color.CYAN)
                                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                                    ? escape(String.join(", ", CONFIG.client.extra.stalk.stalkList))
                                                    : "Stalk list is empty"),
                                            false);
                            return 1;
                        })))
        );
    }
}
