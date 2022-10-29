package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;

import static com.zenith.util.Constants.SYSTEM_DISCONNECT;

public class ReconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
                "reconnect",
                "disconnect and reconnect the proxy client"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("reconnect").executes(c -> {
            Proxy.getInstance().disconnect(SYSTEM_DISCONNECT);
            Proxy.getInstance().cancelAutoReconnect();
            Proxy.getInstance().connect();
        });
    }
}
