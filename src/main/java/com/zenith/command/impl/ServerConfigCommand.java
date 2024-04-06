package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EXECUTOR;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ServerConfigCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "serverConfig",
            CommandCategory.MANAGE,
            "Configures the MC server hosted by the proxy",
            asList(
                "port <port>",
                "ping on/off",
                "ping onlinePlayers on/off",
                "ping maxPlayers <int>",
                "ping lanBroadcast on/off",
                "ping log on/off",
                "timeout on/off",
                "timeout <seconds>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("serverConfig").requires(Command::validateAccountOwner)
            .then(literal("port").then(argument("port", integer(1, 65535)).executes(context -> {
                CONFIG.server.bind.port = getInteger(context, "port");
                context.getSource().getEmbed()
                    .title("Port Set")
                    .description("Restarting server...");
                EXECUTOR.execute(() -> {
                    Proxy.getInstance().stopServer();
                    Proxy.getInstance().startServer();
                });
                return 1;
            })))
            .then(literal("ping")
                      .then(argument("pingToggle", toggle()).executes(context -> {
                          CONFIG.server.ping.enabled = getToggle(context, "pingToggle");
                          context.getSource().getEmbed()
                              .title("Ping Set!");
                          return 1;
                      }))
                      .then(literal("onlinePlayers")
                                .then(argument("onlinePlayersToggle", toggle()).executes(context -> {
                                    CONFIG.server.ping.onlinePlayers = getToggle(context, "onlinePlayersToggle");
                                    context.getSource().getEmbed()
                                        .title("Ping Reports Online Players Set!");
                                    return 1;
                                })))
                      .then(literal("maxPlayers").then(argument("maxPlayers", integer(0)).executes(context -> {
                          CONFIG.server.ping.maxPlayers = getInteger(context, "maxPlayers");
                          context.getSource().getEmbed()
                              .title("Ping Max Players Set!");
                          return 1;
                      })))
                      .then(literal("lanBroadcast")
                                .then(argument("lanBroadcastToggle", toggle()).executes(context -> {
                                    CONFIG.server.ping.lanBroadcast = getToggle(context, "lanBroadcastToggle");
                                    context.getSource().getEmbed()
                                        .title("Ping LAN Broadcast Set!");
                                    return 1;
                                })))
                      .then(literal("log")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.server.ping.logPings = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                        .title("Ping Log " + toggleStrCaps(CONFIG.server.ping.logPings));
                                    return 1;
                                }))))
            .then(literal("timeout")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.extra.timeout.enable = getToggle(c, "toggle");
                          syncTimeout();
                          c.getSource().getEmbed()
                              .title("Server Timeout " + toggleStrCaps(CONFIG.server.extra.timeout.enable));
                          return 1;
                      }))
                      .then(argument("timeout", integer(10, 120)).executes(c -> {
                          CONFIG.server.extra.timeout.seconds = getInteger(c, "timeout");
                          syncTimeout();
                          c.getSource().getEmbed()
                              .title("Server Timeout Set");
                          return 1;
                      })));
    }

    private void syncTimeout() {
        int t = CONFIG.server.extra.timeout.enable ? CONFIG.server.extra.timeout.seconds : 0;
        Proxy.getInstance().getActiveConnections().forEach(connection -> connection.setReadTimeout(t));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .color(Color.CYAN)
            .addField("Port", CONFIG.server.bind.port, false)
            .addField("Ping", toggleStr(CONFIG.server.ping.enabled), false)
            .addField("Ping Reports Online Players", toggleStr(CONFIG.server.ping.onlinePlayers), false)
            .addField("Ping Max Players", CONFIG.server.ping.maxPlayers, false)
            .addField("Ping LAN Broadcast", toggleStr(CONFIG.server.ping.lanBroadcast), false)
            .addField("Ping Log", toggleStr(CONFIG.server.ping.logPings), false)
            .addField("Timeout", CONFIG.server.extra.timeout.enable ? CONFIG.server.extra.timeout.seconds : toggleStr(false), false);
    }
}
