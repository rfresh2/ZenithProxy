package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class AutoTotemCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoTotem", "Automatically equips totems in the offhand", asList(
                "on/off",
                "health <int>"
        ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autototem")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.autoTotem.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("Auto Totem On!")
                            .color(Color.CYAN)
                            .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.autoTotem.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("Auto Totem Off!")
                            .color(Color.CYAN)
                            .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                }))
                .then(literal("health")
                        .then(argument("healthArg", integer(0, 20)).executes(c -> {
                            CONFIG.client.extra.autoTotem.healthThreshold = c.getArgument("healthArg", Integer.class);
                            c.getSource().getEmbedBuilder()
                                    .title("Auto Totem Health Threshold Set!")
                                    .color(Color.CYAN)
                                    .addField("Auto Totem", CONFIG.client.extra.autoTotem.enabled ? "on" : "off", false)
                                    .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                            return 1;
                        })));
    }
}
