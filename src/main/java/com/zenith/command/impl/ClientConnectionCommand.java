package com.zenith.command.impl;

import com.github.steveice10.packetlib.ProxyInfo;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ClientConnectionCommand extends Command {
    private static final Pattern bindAddressPattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "clientConnection",
            CommandCategory.MANAGE,
            "Manages the client's connection configuration",
            asList(
                "proxy on/off",
                "proxy type <type>",
                "proxy host <host>",
                "proxy port <port>",
                "proxy user <user>",
                "proxy password <password>",
                "bindAddress <address>",
                "timeout on/off",
                "timeout <seconds>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("clientConnection").requires(Command::validateAccountOwner)
            .then(literal("proxy")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.connectionProxy.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Client Connection Proxy " + toggleStrCaps(CONFIG.client.connectionProxy.enabled));
                          return 1;
                      }))
                      .then(literal("type")
                                .then(argument("type", wordWithChars()).executes(c -> {
                                    try {
                                        CONFIG.client.connectionProxy.type = ProxyInfo.Type.valueOf(getString(c, "type").toUpperCase());
                                        c.getSource().getEmbed()
                                            .title("Proxy Type Set");
                                        return 1;
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
                                    return 1;
                                })))
                      .then(literal("port")
                                .then(argument("port", integer(1, 65535)).executes(c -> {
                                    CONFIG.client.connectionProxy.port = getInteger(c, "port");
                                    c.getSource().getEmbed()
                                        .title("Proxy Port Set");
                                    return 1;
                                })))
                      .then(literal("user")
                                .then(argument("user", wordWithChars()).executes(c -> {
                                    c.getSource().setSensitiveInput(true);
                                    CONFIG.client.connectionProxy.user = getString(c, "user");
                                    c.getSource().getEmbed()
                                        .title("Proxy Username Set");
                                    return 1;
                                })))
                      .then(literal("password")
                                .then(argument("password", wordWithChars()).executes(c -> {
                                    c.getSource().setSensitiveInput(true);
                                    CONFIG.client.connectionProxy.password = getString(c, "password");
                                    c.getSource().getEmbed()
                                        .title("Proxy Password Set");
                                    return 1;
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
                          return 1;
                      })))
            .then(literal("timeout")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.timeout.enable = getToggle(c, "toggle");
                            syncTimeout();
                            c.getSource().getEmbed()
                                .title("Client Connection Timeout " + toggleStrCaps(CONFIG.client.timeout.enable));
                            return 1;
                      }))
                      .then(argument("seconds", integer(10, 120)).executes(c -> {
                          CONFIG.client.timeout.seconds = getInteger(c, "seconds");
                          syncTimeout();
                          c.getSource().getEmbed()
                              .title("Timeout Set");
                          return 1;
                      })));
    }

    private void syncTimeout() {
        int t = CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : 0;
        var client = Proxy.getInstance().getClient();
        if (client == null) return;
        client.setReadTimeout(t);
    }

    @Override
    public void postPopulate(final Embed embed) {
        embed
            .color(Color.CYAN)
            .addField("Proxy", toggleStr(CONFIG.client.connectionProxy.enabled), false)
            .addField("Proxy Type", CONFIG.client.connectionProxy.type.toString(), false)
            .addField("Proxy Host", CONFIG.client.connectionProxy.host, false)
            .addField("Proxy Port", String.valueOf(CONFIG.client.connectionProxy.port), false)
            .addField("Authentication", CONFIG.client.connectionProxy.password.isEmpty() && CONFIG.client.connectionProxy.user.isEmpty()
                          ? "Off" : "On", false)
            .addField("Bind Address", CONFIG.client.bindAddress, false)
            .addField("Timeout", CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : toggleStr(false), false);
    }
}
