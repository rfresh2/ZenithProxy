package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.Optional;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.PRIORITY_BAN_CHECKER;
import static java.util.Arrays.asList;

public class PrioCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "prio",
                "Configure the mentions for 2b2t priority & priority ban updates",
                asList("mentions on/off", "banMentions on/off", "check")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("prio")
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
                .then(literal("check").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Checking Prio ban")
                            .color(Color.CYAN);
//                    Proxy.getInstance().updatePrioBanStatus();
                    Optional<Boolean> banned = PRIORITY_BAN_CHECKER.checkPrioBan();
                    c.getSource().getEmbedBuilder()
                            .addField("Banned", (banned.isPresent() ? banned.get().toString() : "null"), true);
                }));
    }
}
