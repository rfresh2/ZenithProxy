package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.GitAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class AutoUpdateCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoUpdate", "Configures the autoupdater.",
                asList("on/off"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoupdate").requires(Command::validateAccountOwner)
                .then(literal("on").executes(c -> {
                    CONFIG.autoUpdater.autoUpdate = true;
                    AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                    if (autoUpdater == null) {
                        if (LAUNCH_CONFIG.release_channel.equals("git")) autoUpdater = new GitAutoUpdater();
                        else autoUpdater = new RestAutoUpdater();
                        Proxy.getInstance().setAutoUpdater(autoUpdater);
                    }
                    LAUNCH_CONFIG.auto_update = true;
                    saveLaunchConfig();
                    autoUpdater.start();
                    c.getSource().getEmbedBuilder().title("AutoUpdater On!");
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.autoUpdater.autoUpdate = false;
                    AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                    if (autoUpdater != null) autoUpdater.stop();
                    LAUNCH_CONFIG.auto_update = false;
                    saveLaunchConfig();
                    c.getSource().getEmbedBuilder().title("AutoUpdater Off!");
                }));
    }
}
