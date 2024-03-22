package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.event.proxy.UpdateStartEvent;
import com.zenith.feature.autoupdater.AutoUpdater;
import discord4j.rest.util.Color;

import java.util.Optional;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class UpdateCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "update",
            CommandCategory.CORE,
            "Restarts and updates the proxy software",
            asList("restart", "shutdown", "reboot")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("update").requires(Command::validateAccountOwner).executes(c -> {
            try {
                EVENT_BUS.post(new UpdateStartEvent(Optional.ofNullable(Proxy.getInstance().getAutoUpdater()).flatMap(AutoUpdater::getNewVersion)));
                CONFIG.discord.isUpdating = true;
                if (Proxy.getInstance().isConnected()) {
                    CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
                }
                Proxy.getInstance().stop();
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed to update", e);
                CONFIG.discord.isUpdating = false;
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                c.getSource().getEmbed()
                        .title("Failed updating")
                        .color(Color.RUBY);
            }
            return 1;
        }).then(literal("c").executes(c -> {
            CONFIG.discord.isUpdating = true;
            CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
            EVENT_BUS.post(new UpdateStartEvent(Optional.ofNullable(Proxy.getInstance().getAutoUpdater()).flatMap(AutoUpdater::getNewVersion)));
            Proxy.getInstance().stop();
        }));
    }
}
