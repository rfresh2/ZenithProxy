package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import java.util.List;

import static com.zenith.util.Constants.DISCORD_LOG;
import static java.util.Arrays.asList;

public class DisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
                "disconnect",
                "Disconnect the current player from the server",
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("disconnect").executes(c -> {
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
        });
    }

    @Override
    public List<String> aliases() {
        return asList("dc");
    }
}
