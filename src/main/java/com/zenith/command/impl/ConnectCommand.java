package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DISCORD_LOG;
import static java.util.Arrays.asList;

public class ConnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
                "connect",
                "Connect the current player to the server",
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("connect").executes(c -> {
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
                            .color(Color.RUBY)
                            .addField("Server", CONFIG.client.server.address, true)
                            .addField("Proxy IP", CONFIG.server.getProxyAddress(), false);
                }
            }
        });
    }

    @Override
    public List<String> aliases() {
        return asList("c");
    }
}
