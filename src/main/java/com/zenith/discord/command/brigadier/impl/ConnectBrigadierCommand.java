package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import discord4j.rest.util.Color;

import java.util.Collections;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DISCORD_LOG;

public class ConnectBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "connect",
                "Connect the current player to the server",
                Collections.emptyList()
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("connect").executes(c -> {
                    if (Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbedBuilder()
                                .title("Already Connected!");

                    } else {
                        try {
                            Proxy.getInstance().connect();
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Failed to connect", e);
                            c.getSource().getEmbedBuilder()
                                    .title("Proxy Failed to Connect")
                                    .color(Color.RED)
                                    .addField("Server", CONFIG.client.server.address, true)
                                    .addField("Proxy IP", CONFIG.server.getProxyAddress(), false);
                        }
                    }
                })
        );
    }
}
