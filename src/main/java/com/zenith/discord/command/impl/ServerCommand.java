package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.discord.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class ServerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "server",
                "Change the server the proxy connects to.",
                asList("<IP>", "<IP> <port>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("server")
                .then(argument("ip", wordWithChars()).executes(c -> {
                            final String ip = StringArgumentType.getString(c, "ip");
                            CONFIG.client.server.address = ip;
                            CONFIG.client.server.port = 25565;
                            c.getSource().getEmbedBuilder()
                                    .title("Server Updated!")
                                    .addField("IP", CONFIG.client.server.address, false)
                                    .addField("Port", "" + CONFIG.client.server.port, true)
                                    .color(Color.CYAN);
                            return 1;
                        })
                        .then(argument("port", integer()).executes(c -> {
                            final String ip = StringArgumentType.getString(c, "ip");
                            final int port = IntegerArgumentType.getInteger(c, "port");
                            CONFIG.client.server.address = ip;
                            CONFIG.client.server.port = port;
                            c.getSource().getEmbedBuilder()
                                    .title("Server Updated!")
                                    .addField("IP", CONFIG.client.server.address, false)
                                    .addField("Port", "" + CONFIG.client.server.port, true)
                                    .color(Color.CYAN);
                            return 1;
                        })));
    }
}
