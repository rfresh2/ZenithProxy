package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class QueueWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "queueWarning",
            CommandCategory.INFO,
            "Configure warning messages for when 2b2t queue positions are reached",
            asList("on/off", "position <integer>", "mention on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueWarning")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.queueWarning.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("QueueWarning " + (CONFIG.discord.queueWarning.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("position").then(argument("pos", integer(1, 100)).executes(c -> {
                CONFIG.discord.queueWarning.position = IntegerArgumentType.getInteger(c, "pos");
                c.getSource().getEmbed()
                    .title("Position Updated!");
                return 1;
            })))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.queueWarning.mentionRole = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Mention " + (CONFIG.discord.queueWarning.mentionRole ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("QueueWarning", toggleStr(CONFIG.discord.queueWarning.enabled), false)
            .addField("Position", CONFIG.discord.queueWarning.position, false)
            .addField("Mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
            .color(Color.CYAN);
    }
}
