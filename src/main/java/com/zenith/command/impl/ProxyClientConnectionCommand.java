package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ProxyClientConnectionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "clientConnectionMessages",
                "Send notification messages when a client connects to the proxy",
                asList("on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("clientConnectionMessages")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.clientConnectionMessages = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("Client connection messages " + (CONFIG.client.extra.clientConnectionMessages ? "On!" : "Off!"))
                    .color(Color.CYAN);
                return 1;
            }));
    }
}
