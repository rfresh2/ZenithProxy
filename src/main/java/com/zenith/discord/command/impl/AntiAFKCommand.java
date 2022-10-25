package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "antiAFK",
                "Configure the AntiAFK feature",
                asList("on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("antiAFK")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.enabled = true;
                            c.getSource().getEmbedBuilder()
                                    .title("AntiAFK On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.enabled = false;
                            c.getSource().getEmbedBuilder()
                                    .title("AntiAFK Off!")
                                    .color(Color.CYAN);
                        }))
        );
    }
}
