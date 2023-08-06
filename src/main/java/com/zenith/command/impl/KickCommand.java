package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.network.server.ServerConnection;
import discord4j.rest.util.Color;

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
                "Kick a user from the proxy. Only usable by account owners",
                asList("<player>"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("kick").requires(Command::validateAccountOwner)
                .then(argument("player", string()).executes(c -> {
                    final String playerName = StringArgumentType.getString(c, "player");
                    List<ServerConnection> connections = Proxy.getInstance().getActiveConnections().stream()
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
                }));
    }
}
