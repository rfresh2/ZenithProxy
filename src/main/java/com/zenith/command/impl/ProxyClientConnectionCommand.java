package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class ProxyClientConnectionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "clientConnectionMessages",
                "Send notification messages when a client connects to the proxy",
                asList("on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("clientConnectionMessages")
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
                }));
    }
}
