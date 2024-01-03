package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
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
                "ping lanBroadcast on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("serverConfig").requires(Command::validateAccountOwner)
            .then(literal("port").then(argument("port", integer(1, 65535)).executes(context -> {
                CONFIG.server.bind.port = getInteger(context, "port");
                context.getSource().getEmbedBuilder()
                    .title("Port Set")
                    .description("Restarting server...");
                SCHEDULED_EXECUTOR_SERVICE.execute(() -> {
                    Proxy.getInstance().stopServer();
                    Proxy.getInstance().startServer();
                });
                return 1;
            })))
            .then(literal("ping")
                      .then(argument("pingToggle", toggle()).executes(context -> {
                          CONFIG.server.ping.enabled = getToggle(context, "pingToggle");
                          context.getSource().getEmbedBuilder()
                              .title("Ping Set!");
                          return 1;
                      }))
                      .then(literal("onlinePlayers")
                                .then(argument("onlinePlayersToggle", toggle()).executes(context -> {
                                    CONFIG.server.ping.onlinePlayers = getToggle(context, "onlinePlayersToggle");
                                    context.getSource().getEmbedBuilder()
                                        .title("Ping Reports Online Players Set!");
                                    return 1;
                                })))
                      .then(literal("maxPlayers").then(argument("maxPlayers", integer(0)).executes(context -> {
                          CONFIG.server.ping.maxPlayers = getInteger(context, "maxPlayers");
                          context.getSource().getEmbedBuilder()
                              .title("Ping Max Players Set!");
                          return 1;
                      })))
                      .then(literal("lanBroadcast")
                                .then(argument("lanBroadcastToggle", toggle()).executes(context -> {
                                    CONFIG.server.ping.lanBroadcast = getToggle(context, "lanBroadcastToggle");
                                    context.getSource().getEmbedBuilder()
                                        .title("Ping LAN Broadcast Set!");
                                    return 1;
                                }))));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .color(Color.CYAN)
            .addField("Port", ""+CONFIG.server.bind.port, true)
            .addField("Ping", toggleStr(CONFIG.server.ping.enabled), true)
            .addField("Ping Reports Online Players", toggleStr(CONFIG.server.ping.onlinePlayers), true)
            .addField("Ping Max Players", ""+CONFIG.server.ping.maxPlayers, true)
            .addField("Ping LAN Broadcast", toggleStr(CONFIG.server.ping.lanBroadcast), true);
    }
}
