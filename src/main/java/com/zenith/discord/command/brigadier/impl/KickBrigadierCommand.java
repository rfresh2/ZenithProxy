package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.Proxy;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import com.zenith.server.ServerConnection;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class KickBrigadierCommand extends BrigadierCommand {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "kick",
                "Kick a user from the proxy. Only usable by account owners",
                asList("<player>"));
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("kick").requires(this::validateAccountOwner)
                        .then(argument("player", string()).executes(c -> {
                            final String playerName = StringArgumentType.getString(c, "player");
                            List<ServerConnection> connections = Proxy.getInstance().getServerConnections().stream()
                                    .filter(connection -> connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName))
                                    .collect(Collectors.toList());
                            if (!connections.isEmpty()) {
                                connections.forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                                c.getSource().getEmbedBuilder()
                                        .title("Kicked " + escape(playerName))
                                        .color(Color.CYAN);
                            } else {
                                c.getSource().getEmbedBuilder()
                                        .title("Unable to kick " + escape(playerName))
                                        .color(Color.RUBY)
                                        .addField("Reason", "Player is not connected", false);
                            }
                            return 1;
                        }))
        );
    }
}
