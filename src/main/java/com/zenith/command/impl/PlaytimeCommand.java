package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.util.math.MathHelper;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.util.List;

import static com.zenith.Shared.VC_API;
import static com.zenith.command.CustomStringArgumentType.getString;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class PlaytimeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("playtime",
                                 CommandCategory.INFO,
                                 "Gets the playtime of someone on 2b2t",
                                 asList(
                                     "<playerName>"
                                 ),
                                 aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("playtime")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = getString(c, "playerName");
                VC_API.getPlaytime(playerName)
                    .ifPresentOrElse((response) ->
                                         c.getSource().getEmbedBuilder()
                                             .title("Playtime: " + escape(playerName))
                                             .addField("Playtime", MathHelper.formatDuration(Duration.ofSeconds(response.playtimeSeconds())), false)
                                             .color(Color.CYAN),
                                     () -> c.getSource().getEmbedBuilder()
                                         .title(playerName + " not found")
                                         .color(Color.RUBY));
                return 1;
            }));
    }

    @Override
    public List<String> aliases() {
        return asList("pt");
    }
}
