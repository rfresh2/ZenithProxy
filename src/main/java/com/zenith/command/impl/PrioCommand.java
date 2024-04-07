package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PRIOBAN;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class PrioCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "prio",
            CommandCategory.INFO,
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
                            c.getSource().getEmbed()
                                .title("Prio Mentions " + toggleStrCaps(CONFIG.discord.mentionRoleOnPrioUpdate));
                            return OK;
                        })))
            .then(literal("banMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.mentionRoleOnPrioBanUpdate = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Prio Ban Mentions " + toggleStrCaps(CONFIG.discord.mentionRoleOnPrioBanUpdate));
                            return OK;
                        })))
            .then(literal("check").executes(c -> {
                c.getSource().getEmbed()
                    .title("Checking Prio ban");
                c.getSource().getEmbed()
                    .addField("Banned", (PRIOBAN.checkPrioBan().map(Object::toString).orElse("unknown")), true);
            }));
    }

    @Override
    public void postPopulate(Embed builder) {
        builder
            .addField("Prio Status Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioUpdate), true)
            .addField("Prio Ban Mentions", toggleStr(CONFIG.discord.mentionRoleOnPrioBanUpdate), true)
            .primaryColor();
    }
}
