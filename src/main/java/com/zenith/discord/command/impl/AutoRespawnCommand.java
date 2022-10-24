package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AutoRespawnCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "autoRespawn",
                "Automatically respawn the player after dying.",
                asList("on/off", "delay <milliseconds")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("autoRespawn")
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
                        })))
        );
    }
}
