package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class ServerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "server",
            CommandCategory.MANAGE,
            "Change the server the proxy connects to.",
            asList("<IP>", "<IP> <port>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("server").requires(Command::validateAccountOwner)
            .then(argument("ip", wordWithChars())
                      .then(argument("port", integer()).executes(c -> {
                          final String ip = StringArgumentType.getString(c, "ip");
                          final int port = IntegerArgumentType.getInteger(c, "port");
                          CONFIG.client.server.address = ip;
                          CONFIG.client.server.port = port;
                          c.getSource().getEmbedBuilder()
                              .title("Server Updated!");
                          return 1;
                      }))
                      .executes(c -> {
                          final String ip = StringArgumentType.getString(c, "ip");
                          CONFIG.client.server.address = (ip.equalsIgnoreCase("2b2t") ? "connect.2b2t.org" : ip);
                          CONFIG.client.server.port = 25565;
                          c.getSource().getEmbedBuilder()
                              .title("Server Updated!");
                          return 1;
                      }));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("IP", CONFIG.client.server.address, false)
            .addField("Port", "" + CONFIG.client.server.port, true)
            .color(Color.CYAN);
    }
}
