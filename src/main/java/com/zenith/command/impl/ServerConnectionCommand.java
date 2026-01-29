package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.util.config.Config;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EXECUTOR;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ServerConnectionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("serverConnection")
            .category(CommandCategory.MANAGE)
            .description("""
            Configures the MC server hosted by Zenith and players' connections to it

            The `proxyIP` is the IP players should connect to. This is purely informational.

            The `port` argument changes the port the ZenithProxy MC server listens on

            `upnp` will try to open the port to the public internet, useful for self-hosting on a home network

            The `ping` arguments configure the server list ping response ZenithProxy sends to players.
            `onlinePlayers` = MC profiles of players
            `onlinePlayerCount` = number of players connected
            `maxPlayers` = number of players that can connect
            `lanBroadcast` = LAN server broadcast
            `log` = logs pings

            The `timeout` arguments configures how long until players are kicked due no packets being received.
            """)
            .usageLines(
                "proxyIP <ip>",
                "port <port>",
                "upnp on/off",
                "ping on/off",
                "ping onlinePlayers on/off",
                "ping onlinePlayerCount on/off",
                "ping maxPlayers <int>",
                "ping lanBroadcast on/off",
                "ping log on/off",
                "enforceMatchingConnectingAddress on/off",
                "timeout on/off",
                "timeout <seconds>",
                "autoConnectOnLogin on/off",
                "updateServerIcon on/off",
                "chatSigning mode <disguised/passthrough/system>",
                "preferLoginAsController on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("serverConnection").requires(Command::validateAccountOwner)
            .then(literal("proxyIP").then(argument("ip", wordWithChars()).executes(c -> {
                CONFIG.server.proxyIP = getString(c, "ip");
                c.getSource().getEmbed()
                    .title("Proxy IP Set");
            })))
            .then(literal("port").then(argument("port", integer(1, 65535)).executes(context -> {
                CONFIG.server.bind.port = getInteger(context, "port");
                context.getSource().getEmbed()
                    .title("Port Set")
                    .description("Restarting server...");
                EXECUTOR.execute(() -> {
                    Proxy.getInstance().stopServer();
                    Proxy.getInstance().startServer();
                });
            })))
            .then(literal("upnp").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.upnp = getToggle(c, "toggle");
                if (CONFIG.server.upnp) {
                    Proxy.getInstance().openUpnp();
                } else {
                    Proxy.getInstance().closeUpnp();
                }
                c.getSource().getEmbed()
                    .title("UPnP " + toggleStrCaps(CONFIG.server.upnp));
            })))
            .then(literal("ping")
                .then(argument("pingToggle", toggle()).executes(context -> {
                    CONFIG.server.ping.enabled = getToggle(context, "pingToggle");
                    context.getSource().getEmbed()
                        .title("Ping Set!");
                }))
                .then(literal("onlinePlayers")
                    .then(argument("onlinePlayersToggle", toggle()).executes(context -> {
                        CONFIG.server.ping.onlinePlayers = getToggle(context, "onlinePlayersToggle");
                        context.getSource().getEmbed()
                            .title("Ping Reports Online Players Set!");
                    })))
                .then(literal("onlinePlayerCount")
                    .then(argument("onlinePlayerCountToggle", toggle()).executes(context -> {
                        CONFIG.server.ping.onlinePlayerCount = getToggle(context, "onlinePlayerCountToggle");
                        context.getSource().getEmbed()
                            .title("Ping Online Player Count Set!");
                    })))
                .then(literal("maxPlayers").then(argument("maxPlayers", integer(0)).executes(context -> {
                    CONFIG.server.ping.maxPlayers = getInteger(context, "maxPlayers");
                    context.getSource().getEmbed()
                        .title("Ping Max Players Set!");
                })))
                .then(literal("lanBroadcast")
                    .then(argument("lanBroadcastToggle", toggle()).executes(context -> {
                        CONFIG.server.ping.lanBroadcast = getToggle(context, "lanBroadcastToggle");
                        context.getSource().getEmbed()
                            .title("Ping LAN Broadcast Set!");
                    })))
                .then(literal("log")
                    .then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.server.ping.logPings = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("Ping Log " + toggleStrCaps(CONFIG.server.ping.logPings));
                    }))))
            .then(literal("enforceMatchingConnectingAddress").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.enforceMatchingConnectingAddress = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Enforce Connecting Address " + toggleStrCaps(CONFIG.server.enforceMatchingConnectingAddress));
            })))
            .then(literal("timeout")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.extra.timeout.enable = getToggle(c, "toggle");
                    syncTimeout();
                    c.getSource().getEmbed()
                        .title("Server Timeout " + toggleStrCaps(CONFIG.server.extra.timeout.enable));
                }))
                .then(argument("timeout", integer(1, 120)).executes(c -> {
                    CONFIG.server.extra.timeout.seconds = getInteger(c, "timeout");
                    syncTimeout();
                    c.getSource().getEmbed()
                        .title("Server Timeout Set");
                })))
            .then(literal("autoConnectOnLogin").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoConnectOnLogin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Auto Connect On Login " + toggleStrCaps(CONFIG.client.extra.autoConnectOnLogin));
            })))
            .then(literal("injectTablistFooter").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.injectTablistFooter = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Inject Tablist Footer "  + toggleStrCaps(CONFIG.server.injectTablistFooter));
            })))
            .then(literal("welcomeMessages").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.welcomeMessages = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Welcome Messages " + toggleStrCaps(CONFIG.server.welcomeMessages));
            })))
            .then(literal("updateServerIcon").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.updateServerIcon = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Update Server Icon " + toggleStrCaps(CONFIG.server.updateServerIcon));
            })))
            .then(literal("chatSigning").then(literal("mode").then(argument("mode", enumStrings(Config.Server.ChatSigning.ChatSigningMode.values())).executes(c -> {
                CONFIG.server.chatSigning.mode = Config.Server.ChatSigning.ChatSigningMode.valueOf(getString(c, "mode").toUpperCase());
                c.getSource().getEmbed()
                    .title("Chat Signing Mode Set");
            }))))
            .then(literal("preferLoginAsController").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.preferLoginAsController = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Prefer Login As Controller " + toggleStrCaps(CONFIG.server.preferLoginAsController));
            })));
    }

    private void syncTimeout() {
        int t = CONFIG.server.extra.timeout.enable ? CONFIG.server.extra.timeout.seconds : 0;
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.setReadTimeout(t);
        }
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .primaryColor()
            .addField("Proxy IP", CONFIG.server.proxyIP)
            .addField("Port", CONFIG.server.bind.port)
            .addField("UPnP", CONFIG.server.upnp)
            .addField("Ping", toggleStr(CONFIG.server.ping.enabled))
            .addField("Ping Reports Online Players", toggleStr(CONFIG.server.ping.onlinePlayers))
            .addField("Ping Reports Online Player Count", toggleStr(CONFIG.server.ping.onlinePlayerCount))
            .addField("Ping Max Players", CONFIG.server.ping.maxPlayers)
            .addField("Ping LAN Broadcast", toggleStr(CONFIG.server.ping.lanBroadcast))
            .addField("Ping Log", toggleStr(CONFIG.server.ping.logPings))
            .addField("Enforce Matching Connecting Address", toggleStr(CONFIG.server.enforceMatchingConnectingAddress))
            .addField("Timeout", CONFIG.server.extra.timeout.enable ? CONFIG.server.extra.timeout.seconds : toggleStr(false))
            .addField("Auto Connect On Login", toggleStr(CONFIG.client.extra.autoConnectOnLogin))
            .addField("Prefer Login As Controller", toggleStr(CONFIG.server.preferLoginAsController));
    }
}
