package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.util.math.MathHelper;
import discord4j.rest.util.Color;

import java.time.Duration;

import static com.zenith.Shared.VC;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
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
                                 asList("pt")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("playtime")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = getString(c, "playerName");
                VC.getPlaytime(playerName)
                    .ifPresentOrElse((response) ->
                                         c.getSource().getEmbed()
                                             .title("Playtime")
                                             .addField("Player", playerName, true)
                                             .description(MathHelper.formatDurationLong(Duration.ofSeconds(response.playtimeSeconds())))
                                             .thumbnail(Proxy.getInstance().getAvatarURL(playerName).toString())
                                             .color(Color.CYAN),
                                     () -> c.getSource().getEmbed()
                                         .title(playerName + " not found")
                                         .color(Color.RUBY));
                return 1;
            }));
    }
}
