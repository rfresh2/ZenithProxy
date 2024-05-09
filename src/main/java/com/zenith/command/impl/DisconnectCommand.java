package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.module.impl.AutoReconnect;

import static com.zenith.Shared.DISCORD_LOG;
import static com.zenith.Shared.MODULE;
import static java.util.Arrays.asList;

public class DisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "disconnect",
            CommandCategory.CORE,
            "Disconnects ZenithProxy from the destination MC server",
            asList("dc")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("disconnect").executes(c -> {
            if (!Proxy.getInstance().isConnected()) {
                boolean loginCancelled = Proxy.getInstance().cancelLogin();
                boolean autoReconnectCancelled = MODULE.get(AutoReconnect.class).cancelAutoReconnect();
                if (autoReconnectCancelled) {
                    c.getSource().getEmbed()
                        .title("AutoReconnect Cancelled");
                    return;
                }
                if (loginCancelled) {
                    c.getSource().getEmbed()
                            .title("Login Cancelled");
                    return;
                }
                c.getSource().getEmbed()
                        .title("Already Disconnected!");
            } else {
                try {
                    Proxy.getInstance().disconnect();
                    MODULE.get(AutoReconnect.class).cancelAutoReconnect();
                } catch (final Exception e) {
                    DISCORD_LOG.error("Failed to disconnect", e);
                    c.getSource().getEmbed()
                            .title("Proxy Failed to Disconnect");
                }
            }
        });
    }
}
