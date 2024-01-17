package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.api.vcapi.model.StatsResponse;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.zenith.Shared.VC_API;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.math.MathHelper.formatDuration;
import static java.util.Arrays.asList;

public class StatsCommand extends Command {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("stats",
                                 CommandCategory.INFO,
                                 "Gets the 2b2t stats of a player",
                                 asList(
                                     "<playerName>"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("stats")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = c.getArgument("playerName", String.class);
                final Optional<StatsResponse> statsResponse = VC_API.getStats(playerName);
                if (statsResponse.isEmpty()) {
                    c.getSource().getEmbed()
                        .title(playerName + " not found");
                    return -1;
                }
                final StatsResponse playerStats = statsResponse.get();
                c.getSource().getEmbed()
                    .title("Player Stats: " + escape(playerName))
                    .color(Color.CYAN)
                    .addField("Joins", ""+playerStats.joinCount(), true)
                    .addField("Leaves", ""+playerStats.leaveCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("First Seen", playerStats.firstSeen().format(formatter), true)
                    .addField("Last Seen", playerStats.lastSeen().format(formatter), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Playtime", formatDuration(Duration.ofSeconds(playerStats.playtimeSeconds())), true)
                    .addField("Playtime (Last 30 Days)", formatDuration(Duration.ofSeconds(playerStats.playtimeSecondsMonth())), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Deaths", ""+playerStats.deathCount(), true)
                    .addField("Kills", ""+playerStats.killCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Chats", ""+playerStats.chatsCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("\u200B", "\u200B", true);
                return 1;
            }));
    }
}
