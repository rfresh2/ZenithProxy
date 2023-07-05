package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AutoRespawnCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "autoRespawn",
                "Automatically respawn the player after dying.",
                asList("on/off", "delay <milliseconds>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoRespawn")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.autoRespawn.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("AutoRespawn On!")
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.autoRespawn.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AutoRespawn Off!")
                            .color(Color.CYAN);
                }))
                .then(literal("delay").then(argument("delayMs", integer()).executes(c -> {
                    final int delay = IntegerArgumentType.getInteger(c, "delayMs");
                    CONFIG.client.extra.autoRespawn.delayMillis = delay;
                    c.getSource().getEmbedBuilder()
                            .title("AutoRespawn Delay Updated!")
                            .addField("Status", (CONFIG.client.extra.autoRespawn.enabled ? "on" : "off"), false)
                            .addField("Delay", "" + CONFIG.client.extra.autoRespawn.delayMillis, false)
                            .color(Color.CYAN);
                    return 1;
                })));
    }
}
