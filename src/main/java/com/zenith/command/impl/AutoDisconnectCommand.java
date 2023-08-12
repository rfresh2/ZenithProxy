package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.AutoDisconnect;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static java.util.Arrays.asList;

public class AutoDisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "autoDisconnect",
                "Configures the AutoDisconnect feature",
                asList(
                        "on/off",
                        "health <integer>",
                        "cancelAutoReconnect on/off",
                        "autoClientDisconnect on/off",
                        "thunder on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoDisconnect")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.utility.actions.autoDisconnect.enabled = true;
                    MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                    c.getSource().getEmbedBuilder()
                            .title("AutoDisconnect On!")
                            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                            .addField("CancelAutoReconnect", (CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect ? "on" : "off"), false)
                            .addField("AutoClientDisconnect", (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect ? "on" : "off"), false)
                            .addField("Thunder", (CONFIG.client.extra.utility.actions.autoDisconnect.thunder ? "on" : "off"), false)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.utility.actions.autoDisconnect.enabled = false;
                    MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                    c.getSource().getEmbedBuilder()
                            .title("AutoDisconnect Off!")
                            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                            .addField("CancelAutoReconnect", (CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect ? "on" : "off"), false)
                            .addField("AutoClientDisconnect", (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect ? "on" : "off"), false)
                            .addField("Thunder", (CONFIG.client.extra.utility.actions.autoDisconnect.thunder ? "on" : "off"), false)
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
                .then(literal("cancelAutoReconnect")
                          .then(literal("on").executes(c -> {
                                CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect = true;
                                c.getSource().getEmbedBuilder()
                                        .title("AutoDisconnect CancelAutoReconnect On!")
                                        .color(Color.CYAN);
                          }))
                          .then(literal("off").executes(c -> {
                                CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect = false;
                                c.getSource().getEmbedBuilder()
                                        .title("AutoDisconnect CancelAutoReconnect Off!")
                                        .color(Color.CYAN);
                          })))
                .then(literal("autoClientDisconnect")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = true;
                            MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect AutoClientDisconnect On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = false;
                            MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect AutoClientDisconnect Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("thunder")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = true;
                            MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                            c.getSource().getEmbedBuilder()
                                    .title("AutoDisconnect Thunder On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = false;
                            MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
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
