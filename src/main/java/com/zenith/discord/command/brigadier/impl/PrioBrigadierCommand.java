package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class PrioBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "prio",
                "Configure the mentions for 2b2t priority & priority ban updates",
                asList("mentions on/off", "banMentions on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("prio")
                        .then(literal("mentions")
                                .then(literal("on").executes(c -> {
                                    CONFIG.discord.mentionRoleOnPrioUpdate = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("Prio Mentions On!")
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.discord.mentionRoleOnPrioUpdate = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("Prio Mentions Off!")
                                            .color(Color.CYAN);
                                })))
                        .then(literal("banMentions")
                                .then(literal("on").executes(c -> {
                                    CONFIG.discord.mentionRoleOnPrioBanUpdate = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("Prio Ban Mentions On!")
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.discord.mentionRoleOnPrioBanUpdate = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("Prio Ban Mentions Off!")
                                            .color(Color.CYAN);
                                })))
        );
    }
}
