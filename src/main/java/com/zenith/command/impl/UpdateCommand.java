package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.event.update.UpdateStartEvent;

import static com.zenith.Globals.*;

public class UpdateCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("update")
            .category(CommandCategory.CORE)
            .description("Restarts and updates ZenithProxy if `autoUpdate` is enabled")
            .aliases(
                "restart",
                "reboot"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("update").requires(Command::validateAccountOwner).executes(c -> {
            try {
                if (!LAUNCH_CONFIG.auto_update) {
                    DEFAULT_LOG.error("Auto update is disabled. No update will be applied");
                }
                EVENT_BUS.post(new UpdateStartEvent(Proxy.getInstance().getAutoUpdater().getNewVersion()));
                CONFIG.discord.isUpdating = true;
                if (Proxy.getInstance().isConnected()) {
                    CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
                }
                if (LAUNCH_CONFIG.auto_update && LAUNCH_CONFIG.release_channel.endsWith(".pre")) {
                    // force update when using pre-releases
                    LAUNCH_CONFIG.version = "0.0.0+" + LAUNCH_CONFIG.release_channel;
                    saveLaunchConfig();
                }
                Proxy.getInstance().stop();
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed to update", e);
                CONFIG.discord.isUpdating = false;
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                c.getSource().getEmbed()
                        .title("Failed updating")
                        .errorColor();
            }
            return OK;
        }).then(literal("c").executes(c -> {
            CONFIG.discord.isUpdating = true;
            CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
            if (LAUNCH_CONFIG.auto_update && LAUNCH_CONFIG.release_channel.endsWith(".pre")) {
                // force update when using pre-releases
                LAUNCH_CONFIG.version = "0.0.0+" + LAUNCH_CONFIG.release_channel;
                saveLaunchConfig();
            }
            EVENT_BUS.post(new UpdateStartEvent(Proxy.getInstance().getAutoUpdater().getNewVersion()));
            Proxy.getInstance().stop();
        }));
    }
}
