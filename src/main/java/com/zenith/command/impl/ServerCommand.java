package com.zenith.command.impl;

import com.google.common.primitives.Ints;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.util.ChatUtil;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EXECUTOR;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class ServerCommand extends Command {
    private final Pattern ipWithPortPattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]{1,5}$");
    private final Pattern domainPattern = Pattern.compile("^[a-zA-Z0-9.-]+$");
    private final Pattern domainWithPortPattern = Pattern.compile("^[a-zA-Z0-9.-]+:[0-9]{1,5}$");
    private final Pattern ipv6Pattern = Pattern.compile("^((([0-9A-Fa-f]{1,4}:){1,6}:)|(([0-9A-Fa-f]{1,4}:){7}))([0-9A-Fa-f]{1,4})$");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("server")
            .category(CommandCategory.MANAGE)
            .description("Change the MC server ZenithProxy connects to.")
            .usageLines(
                "<IP>",
                "<IP> <port>",
                "ping <IP>",
                "ping <IP> <port>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("server").requires(Command::validateAccountOwner)
            .then(argument("ip", wordWithChars())
                .then(argument("port", integer(1, 65535)).executes(c -> {
                    final String ip = StringArgumentType.getString(c, "ip");
                    final int port = IntegerArgumentType.getInteger(c, "port");
                    CONFIG.client.server.address = ip;
                    CONFIG.client.server.port = port;
                    c.getSource().getEmbed()
                        .title("Server Updated!");
                    pingServer(c.getSource(), CONFIG.client.server.address, CONFIG.client.server.port, false);
                    return OK;
                }))
                .executes(c -> {
                    final String ip = StringArgumentType.getString(c, "ip");
                    var addr = extractIpAndPort(ip);
                    if (addr == null) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Invalid IP format");
                        return OK;
                    }
                    CONFIG.client.server.address = addr.ip();
                    CONFIG.client.server.port = addr.port();
                    c.getSource().getEmbed()
                        .title("Server Updated!");
                    pingServer(c.getSource(), CONFIG.client.server.address, CONFIG.client.server.port, false);
                    return OK;
                }))
            .then(literal("ping")
                .then(argument("ip", wordWithChars())
                    .then(argument("port", integer(1, 65535)).executes(c -> {
                        final String ip = StringArgumentType.getString(c, "ip");
                        final int port = IntegerArgumentType.getInteger(c, "port");
                        pingServer(c.getSource(), ip, port, true);
                        c.getSource().setNoOutput(true);
                        return OK;
                    }))
                    .executes(c -> {
                        final String ip = StringArgumentType.getString(c, "ip");
                        var addr = extractIpAndPort(ip);
                        if (addr == null) {
                            c.getSource().getEmbed()
                                .title("Error")
                                .description("Invalid IP format");
                            return OK;
                        }
                        pingServer(c.getSource(), addr.ip(), addr.port(), true);
                        c.getSource().setNoOutput(true);
                        return OK;
                    })));
    }

    private void pingServer(CommandContext c, String ip, int port, boolean printPing) {
        EXECUTOR.execute(() -> {
            try {
                var response = MCPing.INSTANCE.ping(ip, port, 3000, true);
                if (printPing) {
                    c.getSource().logEmbed(c, Embed.builder()
                        .title("Server Ping")
                        .addField("MOTD", response.description())
                        .addField("MC Version", response.version().name())
                        .addField("IP", ip)
                        .addField("Port", port)
                    );
                }
            } catch (Throwable e) {
                c.getSource().logEmbed(c, Embed.builder()
                    .title("Server Ping Failed")
                    .description("Double check if you have set the correct server IP")
                    .addField("Error", ChatUtil.constrainChatMessageSize(e.getMessage(), true))
                    .addField("IP", ip)
                    .addField("Port", port)
                    .errorColor());
            }
        });
    }

    private @Nullable Address extractIpAndPort(String ip) {
        if (ipWithPortPattern.matcher(ip).matches()) {
            final String[] split = ip.split(":");
            if (split.length != 2) {
                return null;
            }
            String ipExtracted = split[0];
            Integer p = Ints.tryParse(split[1]);
            if (p == null) {
                return null;
            }
            return new Address(ipExtracted, p);
        } else if (domainWithPortPattern.matcher(ip).matches()) {
            final String[] split = ip.split(":");
            if (split.length != 2) {
                return null;
            }
            String ipExtracted = split[0];
            Integer p = Ints.tryParse(split[1]);
            if (p == null) {
                return null;
            }
            return new Address(ipExtracted, p);
        } else if (ipv6Pattern.matcher(ip).matches()) {
            return new Address(ip, 25565);
        } else if (domainPattern.matcher(ip).matches()) {
            return new Address(ip, 25565);
        } else {
            return null;
        }
    }

    record Address(String ip, int port) {}

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("IP", CONFIG.client.server.address, false)
            .addField("Port", CONFIG.client.server.port, true)
            .primaryColor();
    }
}
