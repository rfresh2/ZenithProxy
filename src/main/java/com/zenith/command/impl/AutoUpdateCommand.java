package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

import static com.zenith.Shared.AUTO_UPDATER;
import static com.zenith.Shared.CONFIG;
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
                    AUTO_UPDATER.start();
                    c.getSource().getEmbedBuilder().title("AutoUpdater On!");
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.autoUpdater.autoUpdate = false;
                    AUTO_UPDATER.stop();
                    c.getSource().getEmbedBuilder().title("AutoUpdater Off!");
                }));
    }
}
