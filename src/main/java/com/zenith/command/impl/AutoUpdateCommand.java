package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.NoOpAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;

import static com.zenith.Shared.LAUNCH_CONFIG;
import static com.zenith.Shared.saveLaunchConfig;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoUpdateCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "autoUpdate",
            CommandCategory.MANAGE,
            """
            Configures the AutoUpdater.
            
            Updates are not immediately applied while the client is connected.
            When an update is found, it will be applied 30 seconds after the next disconnect, or immediately if already disconnected.
            """,
            asList(
                "on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoUpdate").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                final boolean toggle = getToggle(c, "toggle");
                LAUNCH_CONFIG.auto_update = toggle;
                AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                if (toggle) {
                    if (autoUpdater == null) {
                        if (LAUNCH_CONFIG.release_channel.equals("git")) autoUpdater = new NoOpAutoUpdater();
                        else autoUpdater = new RestAutoUpdater();
                        Proxy.getInstance().setAutoUpdater(autoUpdater);
                    }
                    autoUpdater.start();
                } else {
                    if (autoUpdater != null) autoUpdater.stop();
                }
                LAUNCH_CONFIG.auto_update = toggle;
                saveLaunchConfig();
                c.getSource().getEmbed().title("AutoUpdater " + toggleStrCaps(toggle));
                return OK;
            }));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoUpdater", toggleStr(LAUNCH_CONFIG.auto_update), false)
            .primaryColor();
    }
}
