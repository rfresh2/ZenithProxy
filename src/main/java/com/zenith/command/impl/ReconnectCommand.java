package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.module.impl.AutoReconnect;

import static com.zenith.Shared.*;

public class ReconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "reconnect",
            CommandCategory.MANAGE,
            "disconnect and reconnect the proxy client"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("reconnect").executes(c -> {
            EXECUTOR.execute(() -> {
                Proxy.getInstance().disconnect(SYSTEM_DISCONNECT);
                MODULE.get(AutoReconnect.class).cancelAutoReconnect();
                Proxy.getInstance().connect();
            });
        });
    }
}
