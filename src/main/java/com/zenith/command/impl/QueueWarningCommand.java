package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class QueueWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "queueWarning",
                "Configure warning messages for when 2b2t queue positions are reached",
                asList("on/off", "position <integer>", "mention on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueWarning")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.queueWarning.enabled = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("QueueWarning " + (CONFIG.discord.queueWarning.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("position").then(argument("pos", integer()).executes(c -> {
                final int position = IntegerArgumentType.getInteger(c, "pos");
                CONFIG.discord.queueWarning.position = position;
                c.getSource().getEmbedBuilder()
                    .title("Position Updated!");
                return 1;
            })))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.queueWarning.mentionRole = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Mention " + (CONFIG.discord.queueWarning.mentionRole ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public List<String> aliases() {
        return asList("queue", "q");
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("QueueWarning", toggleStr(CONFIG.discord.queueWarning.enabled), false)
            .addField("Position", "" + CONFIG.discord.queueWarning.position, false)
            .addField("Mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
            .color(Color.CYAN);
    }
}
