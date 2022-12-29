package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import static com.zenith.util.Constants.AUTO_UPDATER;
import static com.zenith.util.Constants.CONFIG;
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
                    CONFIG.autoUpdate = true;
                    AUTO_UPDATER.start();
                    c.getSource().getEmbedBuilder().title("AutoUpdater On!");
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.autoUpdate = false;
                    AUTO_UPDATER.stop();
                    c.getSource().getEmbedBuilder().title("AutoUpdater Off!");
                }));
    }
}
