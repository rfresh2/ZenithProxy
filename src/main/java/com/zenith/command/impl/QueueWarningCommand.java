package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class QueueWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "queueWarning",
            CommandCategory.INFO,
            "Configure alerts sent when 2b2t queue positions are reached",
            asList(
                "on/off",
                "position <integer>",
                "mention on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueWarning")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.queueWarning.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("QueueWarning " + toggleStrCaps(CONFIG.discord.queueWarning.enabled));
                return OK;
            }))
            .then(literal("position").then(argument("pos", integer(1, 100)).executes(c -> {
                CONFIG.discord.queueWarning.position = IntegerArgumentType.getInteger(c, "pos");
                c.getSource().getEmbed()
                    .title("Position Updated!");
                return OK;
            })))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.queueWarning.mentionRole = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Mention " + toggleStrCaps(CONFIG.discord.queueWarning.mentionRole));
                            return OK;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("QueueWarning", toggleStr(CONFIG.discord.queueWarning.enabled), false)
            .addField("Position", CONFIG.discord.queueWarning.position, false)
            .addField("Mention", toggleStr(CONFIG.discord.queueWarning.mentionRole), false)
            .primaryColor();
    }
}
