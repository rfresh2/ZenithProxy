package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;

import java.util.Collections;

import static com.zenith.util.Constants.SYSTEM_DISCONNECT;

public class ReconnectBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "reconnect",
                "disconnect and reconnect the proxy client",
                Collections.emptyList()
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("reconnect").executes(c -> {
                    Proxy.getInstance().disconnect(SYSTEM_DISCONNECT);
                    Proxy.getInstance().cancelAutoReconnect();
                    Proxy.getInstance().connect();
                })
        );
    }
}
