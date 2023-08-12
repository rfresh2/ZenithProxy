package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class SpammerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("spammer", "Spams messages", asList(
                "on/off",
                "delayTicks <int>",
                "randomOrder on/off",
                "appendRandom on/off",
                "list",
                "clear",
                "add <message>",
                "addAt <index> <message>",
                "del <index>"),
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spammer")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.spammer.enabled = true;
                    addListDescription(c.getSource().getEmbedBuilder()
                            .color(Color.CYAN)
                            .title("Spammer On!"));
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.spammer.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .color(Color.CYAN)
                            .title("Spammer Off!");
                }))
                .then(literal("delayticks")
                        .then(argument("delayTicks", integer()).executes(c -> {
                            final int delayTicks = IntegerArgumentType.getInteger(c, "delayTicks");
                            CONFIG.client.extra.spammer.delayTicks = delayTicks;
                            c.getSource().getEmbedBuilder()
                                    .title("Spammer Delay Updated!")
                                    .color(Color.CYAN)
                                    .addField("Delay", "" + CONFIG.client.extra.spammer.delayTicks, false);
                            return 1;
                        })))
                .then(literal("randomorder")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.spammer.randomOrder = true;
                            c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Random Order On!");
                            return 1;
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.spammer.randomOrder = false;
                            c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Random Order Off!");
                            return 1;
                        })))
                .then(literal("appendrandom")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.spammer.appendRandom = true;
                            c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Append Random On!");
                            return 1;
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.spammer.appendRandom = false;
                            c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Append Random Off!");
                            return 1;
                        })))
                .then(literal("list").executes(c -> {
                    addListDescription(c.getSource().getEmbedBuilder()
                            .color(Color.CYAN)
                            .title("Spammer Messages"));
                    return 1;
                }))
                .then(literal("clear").executes(c -> {
                    CONFIG.client.extra.spammer.messages.clear();
                    c.getSource().getEmbedBuilder()
                            .color(Color.CYAN)
                            .title("Spammer Messages Cleared!");
                    return 1;
                }))
                .then(literal("add")
                        .then(argument("message", greedyString()).executes(c -> {
                            final String message = StringArgumentType.getString(c, "message");
                            CONFIG.client.extra.spammer.messages.add(message);
                            addListDescription(c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Message Added!"));
                            return 1;
                        })))
                .then(literal("addAt")
                        .then(argument("index", integer())
                                .then(argument("message", greedyString()).executes(c -> {
                                    final int index = IntegerArgumentType.getInteger(c, "index");
                                    final String message = StringArgumentType.getString(c, "message");
                                    CONFIG.client.extra.spammer.messages.add(index, message);
                                    addListDescription(c.getSource().getEmbedBuilder()
                                            .color(Color.CYAN)
                                            .title("Spammer Message Added!"));
                                    return 1;
                                }))))
                .then(literal("del")
                        .then(argument("index", integer()).executes(c -> {
                            final int index = IntegerArgumentType.getInteger(c, "index");
                            CONFIG.client.extra.spammer.messages.remove(index);
                            addListDescription(c.getSource().getEmbedBuilder()
                                    .color(Color.CYAN)
                                    .title("Spammer Message Removed!"));
                            return 1;
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("spam");
    }
    private void addListDescription(final EmbedCreateSpec.Builder embedBuilder) {
        final List<String> messages = new ArrayList<>();
        for (int index = 0; index < CONFIG.client.extra.spammer.messages.size(); index++) {
            messages.add("`" + index + ":` " + CONFIG.client.extra.spammer.messages.get(index));
        }
        embedBuilder.description(String.join("\n", messages));
    }
}
