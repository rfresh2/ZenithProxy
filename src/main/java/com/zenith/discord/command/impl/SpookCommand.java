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

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "spook",
                "Automatically spooks nearby players",
                asList("on/off", "delay <ticks>")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("spook")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.spook.enabled = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Spook On!")
                                    .color(Color.TAHITI_GOLD);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.spook.enabled = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Spook Off!")
                                    .color(Color.TAHITI_GOLD);
                        }))
                        .then(literal("delay").then(argument("delayTicks", integer()).executes(c -> {
                            final int delay = IntegerArgumentType.getInteger(c, "delayTicks");
                            CONFIG.client.extra.spook.tickDelay = (long) delay;
                            c.getSource().getEmbedBuilder()
                                    .title("Spook Delay Updated!")
                                    .addField("Status", (CONFIG.client.extra.spook.enabled ? "on" : "off"), false)
                                    .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " Tick(s)", false)
                                    .color(Color.TAHITI_GOLD);
                            return 1;
                        })))
        );
    }
}
