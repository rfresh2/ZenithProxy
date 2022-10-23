package com.zenith.discord.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;

import static com.zenith.util.Constants.CONFIG;

public class AutoUpdateBrigadierCommand extends BrigadierCommand {

    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                literal("autoupdate")
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
