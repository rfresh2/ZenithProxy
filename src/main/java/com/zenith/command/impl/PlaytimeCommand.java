package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.feature.api.vcapi.VcApi;
import com.zenith.util.math.MathHelper;

import java.time.Duration;

import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class PlaytimeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("playtime")
            .category(CommandCategory.INFO)
            .description("Gets the playtime of a player on 2b2t using https://api.2b2t.vc/")
            .usageLines(
                "<playerName>"
            )
            .aliases("pt")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("playtime")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = getString(c, "playerName");
                VcApi.INSTANCE.getPlaytime(playerName)
                    .ifPresentOrElse((response) ->
                            c.getSource().getEmbed()
                                .title("Playtime")
                                .addField("Player", playerName, true)
                                .description(MathHelper.formatDurationLong(Duration.ofSeconds(response.playtimeSeconds())))
                                .thumbnail(Proxy.getInstance().getPlayerHeadURL(playerName).toString())
                                .primaryColor(),
                        () -> c.getSource().getEmbed()
                            .title(playerName + " not found")
                            .errorColor());
                return OK;
            }));
    }
}
