package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
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
                            .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.autoTotem.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("Auto Totem Off!")
                            .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                }))
                .then(literal("health")
                        .then(argument("healthArg", integer(0, 20)).executes(c -> {
                            CONFIG.client.extra.autoTotem.healthThreshold = c.getArgument("healthArg", Integer.class);
                            c.getSource().getEmbedBuilder()
                                    .title("Auto Totem Health Threshold Set!")
                                    .addField("Auto Totem", CONFIG.client.extra.autoTotem.enabled ? "on" : "off", false)
                                    .addField("Health Threshold", String.valueOf(CONFIG.client.extra.autoTotem.healthThreshold), false);
                            return 1;
                        })));
    }
}
