package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class AutoDisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "autoDisconnect",
                "Configures the AutoDisconnect feature",
                asList("on/off", "health <integer>", "autoClientDisconnect on/off", "thunder on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoDisconnect")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.utility.actions.autoDisconnect.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("AutoDisconnect On!")
                            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.utility.actions.autoDisconnect.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AutoDisconnect Off!")
                            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                            .color(Color.CYAN);
                }))
                .then(literal("health").then(argument("healthLevel", integer()).executes(c -> {
                    CONFIG.client.extra.utility.actions.autoDisconnect.health = IntegerArgumentType.getInteger(c, "healthLevel");
                    c.getSource().getEmbedBuilder()
                            .title("AutoDisconnect Health Updated!")
                            .addField("Status", (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "on" : "off"), false)
                            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                            .color(Color.CYAN);
                    return 1;
                })))
                .then(literal("autoClientDisconnect")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = true;
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect AutoClientDisconnect On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = false;
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect AutoClientDisconnect Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("thunder")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = true;
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect Thunder On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = false;
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect Thunder Off!")
                                    .color(Color.CYAN);
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("autoLog");
    }
}
