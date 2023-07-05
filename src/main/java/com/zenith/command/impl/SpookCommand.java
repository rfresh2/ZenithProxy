package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.util.Config;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "spook",
                "Automatically spooks nearby players",
                asList("on/off", "delay <ticks>", "mode <visualRange/nearest>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spook")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.spook.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("Spook On!")
                            .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                            .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
                            .color(Color.TAHITI_GOLD);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.spook.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("Spook Off!")
                            .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                            .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
                            .color(Color.TAHITI_GOLD);
                }))
                .then(literal("delay").then(argument("delayTicks", integer()).executes(c -> {
                    final int delay = IntegerArgumentType.getInteger(c, "delayTicks");
                    CONFIG.client.extra.spook.tickDelay = (long) delay;
                    c.getSource().getEmbedBuilder()
                            .title("Spook Delay Updated!")
                            .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                            .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
                            .color(Color.TAHITI_GOLD);
                    return 1;
                })))
                .then(literal("mode")
                        .then(literal("nearest").executes(c -> {
                            CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.NEAREST;
                            c.getSource().getEmbedBuilder()
                                    .title("Spook Mode Updated!")
                                    .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                                    .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                                    .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
                                    .color(Color.TAHITI_GOLD);
                        }))
                        .then(literal("visualrange").executes(c -> {
                            CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.VISUAL_RANGE;
                            c.getSource().getEmbedBuilder()
                                    .title("Spook Mode Updated!")
                                    .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                                    .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                                    .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
                                    .color(Color.TAHITI_GOLD);
                        })));
    }
}
