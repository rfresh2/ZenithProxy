package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

import java.util.List;

import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;
import static java.util.Arrays.asList;

public class ConnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "connect",
            CommandCategory.CORE,
            "Connect the current player to the server",
            aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("connect").executes(c -> {
            if (Proxy.getInstance().isConnected()) {
                c.getSource().getEmbedBuilder()
                        .title("Already Connected!");
            } else {
                SCHEDULED_EXECUTOR_SERVICE.execute(Proxy.getInstance()::connectAndCatchExceptions);
            }
        });
    }

    @Override
    public List<String> aliases() {
        return asList("c");
    }
}
