package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import java.util.Collections;

import static com.zenith.util.Constants.DISCORD_LOG;
import static java.util.Arrays.asList;

public class DisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "disconnect",
                "Disconnect the current player from the server",
                Collections.emptyList(),
                asList("dc")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        LiteralCommandNode<CommandContext> node = dispatcher.register(
                command("disconnect").executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        if (Proxy.getInstance().cancelAutoReconnect()) {
                            c.getSource().getEmbedBuilder()
                                    .title("AutoReconnect Cancelled");
                            return;
                        }
                        c.getSource().getEmbedBuilder()
                                .title("Already Disconnected!");
                    } else {
                        try {
                            Proxy.getInstance().disconnect();
                            Proxy.getInstance().cancelAutoReconnect();
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Failed to disconnect", e);
                            c.getSource().getEmbedBuilder()
                                    .title("Proxy Failed to Disconnect");
                        }
                    }
                })
        );
        dispatcher.register(redirect("dc", node));
    }
}
