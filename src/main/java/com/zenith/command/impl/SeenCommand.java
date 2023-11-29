package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.api.model.SeenResponse;
import discord4j.rest.util.Color;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.zenith.Shared.VC_API;
import static com.zenith.command.CustomStringArgumentType.getString;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class SeenCommand extends Command {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("seen",
                                 CommandCategory.INFO,
                                 "Gets the first and last times a player was seen on 2b2t",
                                 asList(
                                     "<playerName>"
                                 ),
                                 asList("firstseen", "lastseen")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("seen")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = getString(c, "playerName");
                Optional<SeenResponse> firstSeen = VC_API.getFirstSeen(playerName);
                Optional<SeenResponse> lastSeen = VC_API.getLastSeen(playerName);
                if (firstSeen.isEmpty() && lastSeen.isEmpty()) {
                    c.getSource().getEmbedBuilder()
                        .title(escape(playerName) + " not found")
                        .color(Color.RUBY);
                    return -1;
                }
                c.getSource().getEmbedBuilder()
                    .title("Seen: " + escape(playerName))
                    .color(Color.CYAN);
                firstSeen.ifPresent((response) -> c.getSource().getEmbedBuilder()
                    .addField("First Seen", response.time().format(formatter), false));
                lastSeen.ifPresent((response) -> c.getSource().getEmbedBuilder()
                    .addField("Last Seen", response.time().format(formatter), false));
                return 1;
            }));
    }
}
