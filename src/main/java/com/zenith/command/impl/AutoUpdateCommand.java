package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.GitAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;
import discord4j.rest.util.Color;

import static com.zenith.Shared.*;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoUpdateCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoUpdate",
                                 CommandCategory.MANAGE,
                                 "Configures the autoupdater.",
                                 asList("on/off"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoupdate").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                final boolean toggle = getToggle(c, "toggle");
                CONFIG.autoUpdater.autoUpdate = toggle;
                AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                if (toggle) {
                    if (autoUpdater == null) {
                        if (LAUNCH_CONFIG.release_channel.equals("git")) autoUpdater = new GitAutoUpdater();
                        else autoUpdater = new RestAutoUpdater();
                        Proxy.getInstance().setAutoUpdater(autoUpdater);
                    }
                    autoUpdater.start();
                } else {
                    if (autoUpdater != null) autoUpdater.stop();
                }
                LAUNCH_CONFIG.auto_update = toggle;
                saveLaunchConfig();
                c.getSource().getEmbed().title("AutoUpdater " + (toggle ? "On!" : "Off!"));
                return 1;
            }));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoUpdater", toggleStr(CONFIG.autoUpdater.autoUpdate), false)
            .color(Color.CYAN);
    }
}
