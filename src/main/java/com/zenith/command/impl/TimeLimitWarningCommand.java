package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class TimeLimitWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "timeLimitWarning",
            CommandCategory.INFO,
            """
            Configure warnings sent when 2b2t time limit is reached.
            """,
            asList(
                "on/off",
                "timeLimit <minutes>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("timeLimit")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.timeLimitWarning.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Time Limit Warning " + toggleStrCaps(CONFIG.client.extra.timeLimitWarning.enabled));
                return OK;
            }))
            .then(literal("timeLimit").then(argument("minutes", integer(1, 1000)).executes(c -> {
                int minutes = getInteger(c, "minutes");
                CONFIG.client.extra.timeLimitWarning.timeLimit = minutes;
                c.getSource().getEmbed().title("Time Limit set to " + minutes + " minutes");
                return OK;
            })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Time Limit Warning", toggleStr(CONFIG.client.extra.timeLimitWarning.enabled), false)
            .description("**Time Limit:** " + CONFIG.client.extra.timeLimitWarning.timeLimit + " minutes")
            .primaryColor();
    }
}
