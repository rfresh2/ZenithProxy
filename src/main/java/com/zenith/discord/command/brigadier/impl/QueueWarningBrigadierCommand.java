package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class QueueWarningBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "queueWarning",
                "Configure warning messages for when 2b2t queue positions are reached",
                asList("on/off", "position <integer>", "mention on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("queueWarning")
                        .then(literal("on").executes(c -> {
                            CONFIG.discord.queueWarning.enabled = true;
                            c.getSource().getEmbedBuilder()
                                    .title("QueueWarning On!")
                                    .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                                    .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.discord.queueWarning.enabled = false;
                            c.getSource().getEmbedBuilder()
                                    .title("QueueWarning Off!")
                                    .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                                    .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                                    .color(Color.CYAN);
                        }))
                        .then(literal("position").then(argument("pos", integer()).executes(c -> {
                            final int position = IntegerArgumentType.getInteger(c, "pos");
                            CONFIG.discord.queueWarning.position = position;
                            c.getSource().getEmbedBuilder()
                                    .title("QueueWarning Position Updated!")
                                    .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                                    .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                                    .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                                    .color(Color.CYAN);
                            return 1;
                        })))
                        .then(literal("mention")
                                .then(literal("on").executes(c -> {
                                    CONFIG.discord.queueWarning.mentionRole = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("QueueWarning Mention On!")
                                            .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                                            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.discord.queueWarning.mentionRole = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("QueueWarning Mention Off!")
                                            .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                                            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                                            .color(Color.CYAN);
                                })))
        );
    }
}
