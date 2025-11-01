package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.feature.gui.GuiBuilder;
import com.zenith.feature.gui.SlotBuilder;
import com.zenith.feature.gui.elements.Slot;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.network.server.ServerSession;
import com.zenith.util.config.Config.Server.Extra.ServerSwitcher.ServerSwitcherServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.GUI;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class ServerSwitcherCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("switch")
            .category(CommandCategory.MODULE)
            .description("""
            Switch the connected player to an alternate MC server.

            Can be used to switch between multiple ZenithProxy instances quickly.

            Servers being switched to must have transfers enabled and be on an MC version >=1.20.6
            """)
            .usageLines(
                "",
                "register <name> <address> <port>",
                "del <name>",
                "clear",
                "list",
                "<name>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("switch")
            .executes(c -> {
                if (c.getSource().getSource() != CommandSources.PLAYER || c.getSource().getInGamePlayerInfo() == null) {
                    c.getSource().getEmbed()
                        .title("Only in-game controlling players can use this command")
                        .errorColor();
                    return ERROR;
                }
                var session = c.getSource().getInGamePlayerInfo().session();
                if (session.getProtocolVersion().olderThan(ProtocolVersion.v1_20_5)) {
                    c.getSource().getEmbed()
                        .title("Unsupported Client MC Version")
                        .description("Unsupported Client MC version. Must be at least 1.20.6");
                    return ERROR;
                }
                c.getSource().setNoOutput(true);
                var serversList = CONFIG.server.extra.serverSwitcher.servers;
                List<Slot> slots = serversList.stream()
                    .map(server ->
                        SlotBuilder.create()
                            // consistent random item per server
                            .item(ItemRegistry.REGISTRY.get(Math.abs(server.name().hashCode() % 300) + 1))
                            .name(Component.text(server.name()))
                            .buttonClickHandler((button, gui, page, event) -> {
                                if (!event.isLeftClick()) return;
                                gui.session().transfer(server.address(), server.port());
                            })
                            .build())
                    .toList();

                var gui = GuiBuilder.create()
                    .session(c.getSource().getInGamePlayerInfo().session())
                    .paginateList(Component.text("Server Switcher"), slots)
                    .build();
                GUI.open(gui);
                return OK;
            })
            .then(literal("register")
                .then(argument("name", wordWithChars())
                    .then(argument("address", wordWithChars())
                        .then(argument("port", integer(1, 65535)).executes(c -> {
                            var name = getString(c, "name");
                            var address = getString(c, "address");
                            var port = getInteger(c, "port");
                            var newServer = new ServerSwitcherServer(name, address, port);
                            var servers = CONFIG.server.extra.serverSwitcher.servers;
                            servers.removeIf(s -> s.name().equalsIgnoreCase(newServer.name()));
                            servers.add(newServer);
                            c.getSource().getEmbed()
                                .title("Server registered");
                        })))))
            .then(literal("del").then(argument("name", wordWithChars()).executes(c -> {
                var name = getString(c, "name");
                CONFIG.server.extra.serverSwitcher.servers.removeIf(s -> s.name().equalsIgnoreCase(name));
                c.getSource().getEmbed()
                    .title("Server removed");
            })))
            .then(literal("clear").executes(c -> {
                CONFIG.server.extra.serverSwitcher.servers.clear();
                c.getSource().getEmbed()
                    .title("Servers Cleared");
            }))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Server List");
                return OK;
            }))
            .then(argument("name", wordWithChars()).requires(context -> validateCommandSource(context, CommandSources.PLAYER)).executes(c -> {
                var name = getString(c, "name");
                var server = CONFIG.server.extra.serverSwitcher.servers.stream()
                    .filter(s -> s.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

                if (server == null) {
                    c.getSource().getEmbed()
                        .title("Server not found");
                    return OK;
                }
                ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
                if (currentPlayer == null) {
                    c.getSource().getEmbed()
                        .title("No player connected to transfer");
                    return OK;
                }
                if (currentPlayer.getProtocolVersion().olderThan(ProtocolVersion.v1_20_5)) {
                    c.getSource().getEmbed()
                        .title("Unsupported Client MC Version")
                        .errorColor()
                        .addField("Client Version", currentPlayer.getProtocolVersion().getName(), false)
                        .addField("Error", "Client version must be at least 1.20.6", false);
                    return OK;
                }
                currentPlayer.transfer(server.address(), server.port());
                c.getSource().getEmbed()
                    .title("Switched To Server")
                    .addField("Destination", "Name: " + server.name() + "\nAddress: " + server.address() + "\nPort: " + server.port(), false);
                return OK;
            }));
    }

    @Override
    public void defaultEmbed(final Embed embed) {
        var str = CONFIG.server.extra.serverSwitcher.servers.stream()
            .map(s -> s.name() + " -> " + s.address() + ":" + s.port())
            .collect(Collectors.joining("\n"));
        if (str.isBlank()) str = "None";
        embed
            .primaryColor()
            .description("**Registered Servers**\n\n" + str + "\n");
    }
}
