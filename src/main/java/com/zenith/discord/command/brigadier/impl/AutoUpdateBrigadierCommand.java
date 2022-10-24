package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AutoUpdateBrigadierCommand extends BrigadierCommand {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of("autoUpdate", "Configures the autoupdater.",
                asList("on/off"));
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("autoupdate")
                        // todo: could also make an argument type for on/off
                        //  would still need to have an if statement in here so won't be all that much shorter
                        .then(literal("on").executes(c -> {
                            CONFIG.autoUpdate = true;
                            Proxy.autoUpdater.start();
                            c.getSource().getEmbedBuilder().title("AutoUpdater On!");
                            return 1;
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.autoUpdate = false;
                            Proxy.autoUpdater.stop();
                            c.getSource().getEmbedBuilder().title("AutoUpdater Off!");
                            return 1;
                        }))
        );
    }
}
