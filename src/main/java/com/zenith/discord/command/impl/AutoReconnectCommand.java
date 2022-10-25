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

public class AutoReconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "autoReconnect",
                "Configure the AutoReconnect feature",
                asList("on/off", "delay <seconds>"));
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(command("autoreconnect")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.autoReconnect.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("AutoReconnect On!")
                            .addField("Delay", "" + CONFIG.client.extra.autoReconnect.delaySeconds, true)
                            .addField("DelayOffline", "" + CONFIG.client.extra.autoReconnect.delaySecondsOffline, true)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.autoReconnect.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AutoReconnect Off!")
                            .addField("Delay", "" + CONFIG.client.extra.autoReconnect.delaySeconds, true)
                            .addField("DelayOffline", "" + CONFIG.client.extra.autoReconnect.delaySecondsOffline, true)
                            .color(Color.CYAN);
                }))
                .then(literal("delay")
                        .then(argument("delaySec", integer()).executes(c -> {
                            CONFIG.client.extra.autoReconnect.delaySeconds = IntegerArgumentType.getInteger(c, "delaySec");
                            c.getSource().getEmbedBuilder()
                                    .title("AutoReconnect Delay Updated!")
                                    .addField("Status", (CONFIG.client.extra.autoReconnect.enabled ? "on" : "off"), false)
                                    .addField("Delay", "" + CONFIG.client.extra.autoReconnect.delaySeconds, true)
                                    .addField("DelayOffline", "" + CONFIG.client.extra.autoReconnect.delaySecondsOffline, true)
                                    .color(Color.CYAN);
                            return 1;
                        }))
                )
        );
    }
}
