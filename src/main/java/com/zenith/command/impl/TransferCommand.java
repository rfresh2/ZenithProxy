package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.network.server.ServerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class TransferCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("transfer")
            .category(CommandCategory.MANAGE)
            .description("""
                 Transfers connected players to a destination MC server.

                 If no player is specified, all currently connected players will be transferred.
                 If no port is specified, it will be looked up via DNS, or default to 25565.
                 """)
            .usageLines(
                "<address>",
                "<address> <playerName>",
                "<address> <port>",
                "<address> <port> <playerName>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("transfer").requires(Command::validateAccountOwner)
            .then(argument("address", wordWithChars())
                .executes(ctx -> {
                    var address = getString(ctx, "address");
                    var port = resolvePort(address);
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    transfer(ctx.getSource(), address, port, connections);
                })
                .then(argument("player", wordWithChars()).executes(ctx -> {
                    var address = getString(ctx, "address");
                    var port = resolvePort(address);
                    var playerName = getString(ctx, "player");
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    List<ServerSession> players = new ArrayList<>();
                    for (int i = 0; i < connections.length; i++) {
                        if (connections[i].getName().equalsIgnoreCase(playerName)) {
                            players.add(connections[i]);
                        }
                    }
                    transfer(ctx.getSource(), address, port, players.toArray(new ServerSession[0]));
                }))
                .then(argument("port", integer(1, 65535))
                    .executes(ctx -> {
                        var address = getString(ctx, "address");
                        var port = getInteger(ctx, "port");
                        var connections = Proxy.getInstance().getActiveConnections().getArray();
                        transfer(ctx.getSource(), address, port, connections);
                    })
                    .then(argument("player", wordWithChars()).executes(ctx -> {
                        var address = getString(ctx, "address");
                        var port = getInteger(ctx, "port");
                        var playerName = getString(ctx, "player");
                        var connections = Proxy.getInstance().getActiveConnections().getArray();
                        List<ServerSession> players = new ArrayList<>();
                        for (int i = 0; i < connections.length; i++) {
                            if (connections[i].getName().equalsIgnoreCase(playerName)) {
                                players.add(connections[i]);
                            }
                        }
                        transfer(ctx.getSource(), address, port, players.toArray(new ServerSession[0]));
                    }))));
    }

    private void transfer(CommandContext ctx, String address, int port, ServerSession... connections) {
        if (connections.length == 0) {
            ctx.getEmbed()
                .title("Error")
                .errorColor()
                .description("No players to transfer.");
            return;
        }
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.transfer(address, port);
        }
        ctx.getEmbed()
            .title("Transferred " + connections.length + " player(s)")
            .primaryColor()
            .addField("Address", address)
            .addField("Port", port)
            .addField("Players", String.join(", ", Stream.of(connections).map(ServerSession::getName).toList()));
    }

    private int resolvePort(String address) {
        try {
            var resolvedAddress = MCPing.INSTANCE.resolveAddress(address, 25565);
            return resolvedAddress.getPort();
        } catch (Exception e) {
            return 25565;
        }
    }
}
