package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.api.vcapi.model.StatsResponse;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.zenith.Shared.VC_API;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.util.math.MathHelper.formatDurationLong;
import static discord4j.common.util.TimestampFormat.SHORT_DATE_TIME;
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
                    .title("Player Stats")
                    .color(Color.CYAN)
                    .addField("Player", playerName, true)
                    .addField("\u200B", "\u200B", true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Joins", playerStats.joinCount(), true)
                    .addField("Leaves", playerStats.leaveCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("First Seen", SHORT_DATE_TIME.format(playerStats.firstSeen().toInstant()), true)
                    .addField("Last Seen", SHORT_DATE_TIME.format(playerStats.lastSeen().toInstant()), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Playtime", formatDurationLong(Duration.ofSeconds(playerStats.playtimeSeconds())), true)
                    .addField("Playtime (Last 30 Days)", formatDurationLong(Duration.ofSeconds(playerStats.playtimeSecondsMonth())), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Deaths", playerStats.deathCount(), true)
                    .addField("Kills", playerStats.killCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("Chats", playerStats.chatsCount(), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("\u200B", "\u200B", true)
                    .thumbnail(Proxy.getInstance().getAvatarURL(playerName).toString());
                return 1;
            }));
    }
}
