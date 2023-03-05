package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class QueueWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "queueWarning",
                "Configure warning messages for when 2b2t queue positions are reached",
                asList("on/off", "position <integer>", "mention on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueWarning")
                .then(literal("on").executes(c -> {
                    CONFIG.discord.queueWarning.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("QueueWarning On!")
                            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                            .addField("Mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.discord.queueWarning.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("QueueWarning Off!")
                            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                            .addField("Mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                            .color(Color.CYAN);
                }))
                .then(literal("position").then(argument("pos", integer()).executes(c -> {
                    final int position = IntegerArgumentType.getInteger(c, "pos");
                    CONFIG.discord.queueWarning.position = position;
                    c.getSource().getEmbedBuilder()
                            .title("QueueWarning Position Updated!")
                            .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
                            .addField("Mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
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
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("queue", "q");
    }
}
