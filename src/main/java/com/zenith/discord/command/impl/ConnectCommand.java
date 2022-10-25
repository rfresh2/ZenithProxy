package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DISCORD_LOG;
import static java.util.Arrays.asList;

public class ConnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
                "connect",
                "Connect the current player to the server",
                asList("c")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        LiteralCommandNode<CommandContext> node = dispatcher.register(
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
        dispatcher.register(redirect("c", node));
    }
}
