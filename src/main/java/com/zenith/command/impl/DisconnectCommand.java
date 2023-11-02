package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

import java.util.List;

import static com.zenith.Shared.DISCORD_LOG;
import static java.util.Arrays.asList;

public class DisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "disconnect",
            CommandCategory.CORE,
            "Disconnect the current player from the server",
            aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("disconnect").executes(c -> {
            if (!Proxy.getInstance().isConnected()) {
                boolean loginCancelled = Proxy.getInstance().cancelLogin();
                boolean autoReconnectCancelled = Proxy.getInstance().cancelAutoReconnect();
                if (autoReconnectCancelled) {
                    c.getSource().getEmbedBuilder()
                        .title("AutoReconnect Cancelled");
                    return;
                }
                if (loginCancelled) {
                    c.getSource().getEmbedBuilder()
                            .title("Login Cancelled");
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
