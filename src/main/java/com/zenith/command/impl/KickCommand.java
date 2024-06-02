package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.discord.DiscordBot;

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
            """
            Kicks all players or a specific player. Only usable by account owners.
            """,
            asList(
                "",
                "<player>"
            ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("kick").requires(Command::validateAccountOwner)
            .executes(c -> {
                final boolean kickCurrentPlayer = c.getSource().getSource() != CommandSource.IN_GAME_PLAYER;
                final List<String> kickedPlayers = new ArrayList<>();
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    if (connection.equals(Proxy.getInstance().getCurrentPlayer().get()) && !kickCurrentPlayer) continue;
                    kickedPlayers.add(connection.getProfileCache().getProfile().getName());
                    connection.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                }
                c.getSource().getEmbed()
                    .title("Kicked " + kickedPlayers.size() + " players")
                    .addField("Players", kickedPlayers.stream().map(DiscordBot::escape).collect(Collectors.joining(", ")), false);
                return OK;
            })
            .then(argument("player", string()).executes(c -> {
                final String playerName = StringArgumentType.getString(c, "player");
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    if (connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName)) {
                        connection.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                        c.getSource().getEmbed()
                            .title("Kicked " + escape(playerName))
                            .primaryColor();
                        return OK;
                    }
                }
                c.getSource().getEmbed()
                    .title("Unable to kick " + escape(playerName))
                    .errorColor()
                    .addField("Reason", "Player is not connected", false);
                return OK;
            }));
    }
}
