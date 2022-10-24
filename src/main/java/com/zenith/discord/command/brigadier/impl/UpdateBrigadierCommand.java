package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import com.zenith.event.proxy.UpdateStartEvent;
import discord4j.rest.util.Color;

import java.util.Collections;

import static com.zenith.util.Constants.*;

public class UpdateBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "update",
                "Restarts and updates the proxy software",
                Collections.emptyList()
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("update").requires(this::validateAccountOwner).executes(c -> {
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
                })
        );
    }
}
