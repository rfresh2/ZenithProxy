package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import com.zenith.event.proxy.UpdateStartEvent;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.*;

public class UpdateCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
                "update",
                "Restarts and updates the proxy software"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("update").requires(Command::validateAccountOwner).executes(c -> {
            try {
                EVENT_BUS.dispatch(new UpdateStartEvent());
                CONFIG.discord.isUpdating = true;
                if (Proxy.getInstance().isConnected()) {
                    CONFIG.shouldReconnectAfterAutoUpdate = true;
                }
                Proxy.getInstance().stop();
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed to update", e);
                CONFIG.discord.isUpdating = false;
                CONFIG.shouldReconnectAfterAutoUpdate = false;
                saveConfig();
                c.getSource().getEmbedBuilder()
                        .title("Failed updating")
                        .color(Color.RED);
            }
            return 1;
        });
    }
}
