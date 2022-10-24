package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AutoUpdateCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of("autoUpdate", "Configures the autoupdater.",
                asList("on/off"));
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("autoupdate").requires(this::validateAccountOwner)
                        // todo: could also make an argument type for on/off
                        //  would still need to have an if statement in here so won't be all that much shorter
                        .then(literal("on").executes(c -> {
                            CONFIG.autoUpdate = true;
                            Proxy.autoUpdater.start();
                            c.getSource().getEmbedBuilder().title("AutoUpdater On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.autoUpdate = false;
                            Proxy.autoUpdater.stop();
                            c.getSource().getEmbedBuilder().title("AutoUpdater Off!");
                        }))
        );
    }
}
