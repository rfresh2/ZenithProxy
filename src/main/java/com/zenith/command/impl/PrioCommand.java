package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PRIORITY_BAN_CHECKER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class PrioCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "prio",
                "Configure the mentions for 2b2t priority & priority ban updates",
                asList("mentions on/off", "banMentions on/off", "check")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("prio")
            .then(literal("mentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.mentionRoleOnPrioUpdate = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Prio Mentions " + (CONFIG.discord.mentionRoleOnPrioUpdate ? "On!" : "Off!"));
                            return 1;
                        })))
            .then(literal("banMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.mentionRoleOnPrioBanUpdate = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Prio Ban Mentions " + (CONFIG.discord.mentionRoleOnPrioBanUpdate ? "On!" : "Off!"));
                            return 1;
                        })))
            .then(literal("check").executes(c -> {
                c.getSource().getEmbedBuilder()
                    .title("Checking Prio ban");
                c.getSource().getEmbedBuilder()
                    .addField("Banned", (PRIORITY_BAN_CHECKER.checkPrioBan().map(Object::toString).orElse("unknown")), true);
            }));
    }

    @Override
    public void postPopulate(EmbedCreateSpec.Builder builder) {
        builder
            .addField("Prio Status Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioUpdate), true)
            .addField("Prio Ban Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioBanUpdate), true)
            .color(Color.CYAN);
    }
}
