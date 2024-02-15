package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.*;
import com.zenith.discord.DiscordBot;
import com.zenith.network.server.ServerConnection;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class KickCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "kick",
            CommandCategory.MANAGE,
            "Kick a user from the proxy. Only usable by account owners",
            asList("<player>"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("kick").requires(Command::validateAccountOwner)
            .executes(c -> {
                final boolean kickCurrentPlayer = c.getSource().getSource() != CommandSource.IN_GAME_PLAYER;
                final List<String> kickedPlayers = new ArrayList<>();
                for (ServerConnection connection : Proxy.getInstance().getActiveConnections()) {
                    if (connection.equals(Proxy.getInstance().getCurrentPlayer().get()) && !kickCurrentPlayer) continue;
                    kickedPlayers.add(connection.getProfileCache().getProfile().getName());
                    connection.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                }
                c.getSource().getEmbed()
                    .title("Kicked " + kickedPlayers.size() + " players")
                    .addField("Players", kickedPlayers.stream().map(DiscordBot::escape).collect(Collectors.joining(", ")), false);
                return 1;
            })
            .then(argument("player", string()).executes(c -> {
                final String playerName = StringArgumentType.getString(c, "player");
                List<ServerConnection> connections = Proxy.getInstance().getActiveConnections().stream()
                    .filter(connection -> connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName))
                    .collect(Collectors.toList());
                if (!connections.isEmpty()) {
                    connections.forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                    c.getSource().getEmbed()
                        .title("Kicked " + escape(playerName))
                        .color(Color.CYAN);
                } else {
                    c.getSource().getEmbed()
                        .title("Unable to kick " + escape(playerName))
                        .color(Color.RUBY)
                        .addField("Reason", "Player is not connected", false);
                }
                return 1;
            }));
    }
}
