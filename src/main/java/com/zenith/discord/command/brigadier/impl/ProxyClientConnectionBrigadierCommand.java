package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class ProxyClientConnectionBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "clientConnectionMessages",
                "Send notification messages when a client connects to the proxy",
                asList("on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("clientConnectionMessages")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.clientConnectionMessages = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Client connection messages On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.clientConnectionMessages = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Client connection messages Off!")
                                    .color(Color.CYAN);
                        }))
        );
    }
}
