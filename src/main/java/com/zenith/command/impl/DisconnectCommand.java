package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.module.impl.AutoReconnect;

import static com.zenith.Globals.DISCORD_LOG;
import static com.zenith.Globals.MODULE;

public class DisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("disconnect")
            .category(CommandCategory.CORE)
            .description("Disconnects ZenithProxy from the destination MC server")
            .aliases("dc")
            .build();
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
                    c.getSource().setNoOutput(true);
                } catch (final Exception e) {
                    DISCORD_LOG.error("Failed to disconnect", e);
                    c.getSource().getEmbed()
                            .title("Proxy Failed to Disconnect");
                }
            }
        });
    }
}
