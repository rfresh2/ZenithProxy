package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

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
                var seenResponse = VC_API.getSeen(playerName);
                if (seenResponse.isEmpty()) {
                    c.getSource().getEmbedBuilder()
                        .title(escape(playerName) + " not found")
                        .color(Color.RUBY);
                    return -1;
                }
                c.getSource().getEmbedBuilder()
                    .title("Seen: " + escape(playerName))
                    .color(Color.CYAN);
                seenResponse.ifPresent((response) -> c.getSource().getEmbedBuilder()
                    .addField("First Seen", getSeenString(response.firstSeen()), false)
                    .addField("Last Seen", getSeenString(response.lastSeen()), false));
                return 1;
            }));
    }

    private String getSeenString(@Nullable final OffsetDateTime time) {
        return time != null ? time.format(formatter) : "Never";
    }
}
