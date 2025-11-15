package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.util.config.Config.Client.KeepAliveHandling.KeepAliveMode;
import org.geysermc.mcprotocollib.network.ProxyInfo;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ClientConnectionCommand extends Command {
    private static final Pattern bindAddressPattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("clientConnection")
            .category(CommandCategory.MANAGE)
            .description("""
            Manages the connection configuration from ZenithProxy to the destination MC server.
            """)
            .usageLines(
                "autoConnect on/off",
                "proxy on/off",
                "proxy type <type>",
                "proxy host <host>",
                "proxy port <port>",
                "proxy user <user>",
                "proxy password <password>",
                "proxy auth clear",
                "bindAddress <address>",
                "timeout on/off",
                "timeout <seconds>",
                "ping packetInterval <seconds>",
                "keepAlive mode <passthrough/independent>",
                "keepAlive queueTimeout <ms>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("clientConnection").requires(Command::validateAccountOwner)
            .then(literal("autoConnect")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.autoConnect = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Auto Connect " + toggleStrCaps(CONFIG.client.autoConnect));
                    return OK;
                })))
            .then(literal("proxy")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.connectionProxy.enabled = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Client Connection Proxy " + toggleStrCaps(CONFIG.client.connectionProxy.enabled));
                    return OK;
                }))
                .then(literal("type")
                    .then(argument("type", enumStrings(ProxyInfo.Type.values())).executes(c -> {
                        try {
                            CONFIG.client.connectionProxy.type = ProxyInfo.Type.valueOf(getString(c, "type").toUpperCase());
                            c.getSource().getEmbed()
                                .title("Proxy Type Set");
                            return OK;
                        } catch (final Exception e) {
                            c.getSource().getEmbed()
                                .title("Invalid Proxy Type")
                                .addField("Valid Types", Arrays.toString(ProxyInfo.Type.values()), false);
                            return 0;
                        }
                    })))
                .then(literal("host")
                    .then(argument("host", wordWithChars()).executes(c -> {
                        CONFIG.client.connectionProxy.host = getString(c, "host");
                        c.getSource().getEmbed()
                            .title("Proxy Host Set");
                        return OK;
                    })))
                .then(literal("port")
                    .then(argument("port", integer(1, 65535)).executes(c -> {
                        CONFIG.client.connectionProxy.port = getInteger(c, "port");
                        c.getSource().getEmbed()
                            .title("Proxy Port Set");
                        return OK;
                    })))
                .then(literal("auth").then(literal("clear").executes(c -> {
                    CONFIG.client.connectionProxy.user = "";
                    CONFIG.client.connectionProxy.password = "";
                    c.getSource().getEmbed()
                        .title("Proxy User and Password Cleared");
                    return OK;
                })))
                .then(literal("user")
                    .then(argument("user", wordWithChars()).executes(c -> {
                        c.getSource().setSensitiveInput(true);
                        CONFIG.client.connectionProxy.user = getString(c, "user");
                        c.getSource().getEmbed()
                            .title("Proxy Username Set");
                        return OK;
                    })))
                .then(literal("password")
                    .then(argument("password", wordWithChars()).executes(c -> {
                        c.getSource().setSensitiveInput(true);
                        CONFIG.client.connectionProxy.password = getString(c, "password");
                        c.getSource().getEmbed()
                            .title("Proxy Password Set");
                        return OK;
                    }))))
            .then(literal("bindAddress")
                .then(argument("address", wordWithChars()).executes(c -> {
                    var address = getString(c, "address");
                    if (!bindAddressPattern.matcher(address).matches()) {
                        c.getSource().getEmbed()
                            .title("Invalid Bind Address")
                            .addField("Valid Format", "Must be formatted like an IP address, e.g. '0.0.0.0'", false);
                        return 0;
                    }
                    CONFIG.client.bindAddress = address;
                    c.getSource().getEmbed()
                        .title("Bind Address Set");
                    return OK;
                })))
            .then(literal("timeout")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.timeout.enable = getToggle(c, "toggle");
                    syncTimeout();
                    c.getSource().getEmbed()
                        .title("Client Connection Timeout " + toggleStrCaps(CONFIG.client.timeout.enable));
                    return OK;
                }))
                .then(argument("seconds", integer(10, 120)).executes(c -> {
                    CONFIG.client.timeout.seconds = getInteger(c, "seconds");
                    syncTimeout();
                    c.getSource().getEmbed()
                        .title("Timeout Set");
                    return OK;
                })))
            .then(literal("ping")
                .then(literal("packetInterval").then(argument("seconds", integer(1)).executes(c -> {
                    CONFIG.client.ping.pingIntervalSeconds = getInteger(c, "seconds");
                    c.getSource().getEmbed()
                        .title("Ping Packet Interval Set");
                    return OK;
                }))))
            .then(literal("reconfiguringNotification").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.reconfiguringNotification = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Reconfiguring Notification " + toggleStrCaps(CONFIG.client.extra.reconfiguringNotification));
            })))
            .then(literal("keepAlive")
                .then(literal("mode").then(argument("keepAliveMode", enumStrings(KeepAliveMode.values())).executes(c -> {
                    CONFIG.client.keepAliveHandling.keepAliveMode = KeepAliveMode.valueOf(getString(c, "keepAliveMode").toUpperCase());
                    c.getSource().getEmbed()
                        .title("Keep Alive Mode Set");
                })))
                .then(literal("queueTimeout").then(argument("ms", integer(1)).executes(c -> {
                    CONFIG.client.keepAliveHandling.keepAliveQueueTimeoutMs = getInteger(c, "ms");
                    c.getSource().getEmbed()
                        .title("Keep Alive Queue Timeout Set");
                }))));
    }

    private void syncTimeout() {
        int t = CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : 0;
        var client = Proxy.getInstance().getClient();
        if (client == null) return;
        client.setReadTimeout(t);
    }

    @Override
    public void defaultEmbed(final Embed embed) {
        embed
            .primaryColor()
            .addField("Auto Connect", toggleStr(CONFIG.client.autoConnect))
            .addField("Proxy", toggleStr(CONFIG.client.connectionProxy.enabled))
            .addField("Proxy Type", CONFIG.client.connectionProxy.type.toString())
            .addField("Proxy Host", CONFIG.client.connectionProxy.host)
            .addField("Proxy Port", String.valueOf(CONFIG.client.connectionProxy.port))
            .addField("Authentication", CONFIG.client.connectionProxy.password.isEmpty() && CONFIG.client.connectionProxy.user.isEmpty()
                ? "Off" : "On")
            .addField("Bind Address", CONFIG.client.bindAddress)
            .addField("Timeout", CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : toggleStr(false))
            .addField("Ping Packet Interval", CONFIG.client.ping.pingIntervalSeconds + "s")
            .addField("KeepAlive Mode", CONFIG.client.keepAliveHandling.keepAliveMode.name().toLowerCase())
            .addField("KeepAlive Queue Timeout", CONFIG.client.keepAliveHandling.keepAliveQueueTimeoutMs + "ms");
    }
}
