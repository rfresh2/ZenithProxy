package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.feature.api.mcstatus.MCStatusApi;

import java.util.function.Supplier;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ConnectionTestCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "connectionTest",
            CommandCategory.INFO,
            """
            Tests whether this proxy or another MC server is accessible from the public internet.
            
            If the test succeeds, that means other people can connect.
            
            If the test fails, either the `proxyIP` setting is not set to a public IP address or your instance is not
            exposed on the public internet.
            
            To configure the `proxyIP` use the `help serverConnection` command
            
            On a VPS this is usually due to a firewall needing to be disabled.
            
            On a home PC you would need both disable any firewall and configure port forwarding in your router.
            """,
            asList(
                "",
                "<address>",
                "testOnStart on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("connectionTest")
            .executes(c -> {
                if (!CONFIG.server.enabled) {
                    c.getSource().getEmbed()
                        .title("ZenithProxy Server Is Disabled")
                        .errorColor()
                        .description("""
                            Not running connection test.
                            
                            Enable the server in the config.json
                            """);
                    return;
                }
                if (Proxy.getInstance().getServer() == null || !Proxy.getInstance().getServer().isListening()) {
                    c.getSource().getEmbed()
                        .title("ZenithProxy Server Not Listening")
                        .errorColor()
                        .description("""
                            Not running connection test.
                            
                            The ZenithProxy server is not listening for connections
                            """);
                    return;
                }
                if (!CONFIG.server.ping.enabled) {
                    c.getSource().getEmbed()
                        .title("Pings Disabled")
                        .errorColor()
                        .description("""
                            Pings must be enabled to perform the connection test.
                            
                            See `help serverConnection` to configure pings
                            """);
                    return;
                }
                var proxyAddress = CONFIG.server.getProxyAddress();
                if (proxyAddress.startsWith("localhost")) {
                    // we could still test if we queried for our own IP
                    c.getSource().getEmbed()
                        .title("Connection Test Failed")
                        .errorColor()
                        .description(
                            """
                            The `proxyIP` you have configured is currently set to `localhost`.
                            
                            This means you are either hosting ZenithProxy on your home PC or you have not set the public IP yet.
                            
                            To configure the `proxyIP` use the `help serverConnection` command.
                            """);
                    return;
                }
                executeConnectionTest(
                    c.getSource().getEmbed(),
                    proxyAddress,
                    () -> """
                        Unable to connect to configured `proxyIP`: %s
                             
                        This test is most likely failing due to your firewall needing to be disabled.
                        
                        For instructions on how to disable the firewall consult with your VPS provider. Each provider varies in steps.
                         """.formatted(proxyAddress),
                    () -> """
                         Internal error while querying the MC server status API.
                        
                         This issue is not related to your proxy being inaccessible, try again later.
                         """);
            })
            .then(argument("address", wordWithChars()).executes(c -> {
                var proxyAddress = getString(c, "address");
                executeConnectionTest(
                    c.getSource().getEmbed(),
                    proxyAddress,
                    () -> """
                        Unable to connect to: %s
                        """.formatted(proxyAddress),
                    () -> """
                        Internal error while querying the MC server status API.
                        
                        This issue is not related to the server being inaccessible, try again later.
                        """);
                return OK;
            }))
            .then(literal("testOnStart")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.connectionTestOnStart = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Connection Test On Start " + toggleStrCaps(CONFIG.server.connectionTestOnStart))
                              .primaryColor();
                          return OK;
                      })));
    }

    private void executeConnectionTest(
        Embed embed,
        String address,
        Supplier<String> descriptionOnFailure,
        Supplier<String> descriptionOnInternalFailure) {
        MCStatusApi.INSTANCE.getMCServerStatus(address).ifPresentOrElse(response -> {
            if (response.online()) {
                embed
                    .title("Connection Test Successful")
                    .successColor()
                    .addField("Address", address, false)
                    .addField("Host Response", response.host(), false)
                    .addField("Port Response", response.port(), false);
            } else {
                embed
                    .title("Connection Test Failed")
                    .errorColor()
                    .description(descriptionOnFailure.get())
                    .addField("Address", address, false);
            }
        }, () -> {
            embed
                .title("Connection Test Error")
                .errorColor()
                .description(descriptionOnInternalFailure.get())
                .addField("Address", address, false);
        });
    }
}
