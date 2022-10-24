package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class ServerBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "server",
                "Change the server the proxy connects to.",
                asList("<IP>", "<IP> <port>")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("server")
                        .then(argument("ip", string()).executes(c -> {
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
                                })))
        );
    }
}
