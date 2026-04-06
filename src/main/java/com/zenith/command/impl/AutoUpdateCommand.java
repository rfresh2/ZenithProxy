package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.autoupdater.AutoUpdater;

import static com.zenith.Globals.LAUNCH_CONFIG;
import static com.zenith.Globals.saveLaunchConfig;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoUpdateCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoUpdate")
            .category(CommandCategory.MANAGE)
            .description("""
            Configures the AutoUpdater.

            Updates are not immediately applied while the client is connected.
            When an update is found, it will be applied 30 seconds after the next disconnect, or immediately if already disconnected.
            """)
            .usageLines(
                "on/off",
                "launcher on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoUpdate").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                final boolean toggle = getToggle(c, "toggle");
                LAUNCH_CONFIG.auto_update = toggle;
                AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                if (toggle) autoUpdater.start();
                else autoUpdater.stop();
                LAUNCH_CONFIG.auto_update = toggle;
                saveLaunchConfig();
                c.getSource().getEmbed()
                    .title("AutoUpdate " + toggleStrCaps(toggle));
            }))
            .then(literal("launcher").then(argument("launcherToggle", toggle()).executes(c -> {
                LAUNCH_CONFIG.auto_update_launcher = getToggle(c, "launcherToggle");
                saveLaunchConfig();
                c.getSource().getEmbed()
                    .title("Launcher AutoUpdate " + toggleStrCaps(LAUNCH_CONFIG.auto_update_launcher));
            })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("AutoUpdate", toggleStr(LAUNCH_CONFIG.auto_update))
            .addField("Launcher AutoUpdate", toggleStr(LAUNCH_CONFIG.auto_update_launcher))
            .primaryColor();
    }
}
