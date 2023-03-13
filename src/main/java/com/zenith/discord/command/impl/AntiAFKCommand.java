package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "antiAFK",
                "Configure the AntiAFK feature",
                asList("on/off", "safeWalk on/off", "gravity on/off", "safeGravity on/off",
                        "stuckWarning on/off", "stuckWarning mention on/off", "walkDistance <int>",
                        "antiStuck on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.antiafk.enabled = true;
                    defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                            .title("AntiAFK On!");
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.antiafk.enabled = false;
                    defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                            .title("AntiAFK Off!");
                }))
                .then(literal("safewalk")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeWalk = true;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("SafeWalk On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeWalk = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("SafeWalk Off!");
                        })))
                .then(literal("gravity")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.gravity = true;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("Gravity On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.gravity = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("Gravity Off!");
                        })))
                .then(literal("safegravity")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeGravity = true;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("SafeGravity On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeGravity = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("SafeGravity Off!");
                        })))
                .then(literal("stuckwarning")
                        .then(literal("mention")
                                .then(literal("on").executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.stuckWarningMention = true;
                                    defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                            .title("Stuck Warning Mention On!");
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.stuckWarningMention = false;
                                    defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                            .title("Stuck Warning Mention Off!");
                                })))
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.stuckWarning = true;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("Stuck Warning On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.stuckWarning = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("Stuck Warning Off!");
                        })))
                .then(literal("walkdistance")
                        .then(argument("walkdist", integer(1)).executes(c -> {
                            CONFIG.client.extra.antiafk.actions.walkDistance = IntegerArgumentType.getInteger(c, "walkdist");
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("Walk Distance Set!");
                            return 1;
                        })))
                .then(literal("antistuck")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.antiStuck = true;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("AntiStuck Suicide On!");
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.antiStuck = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                    .title("AntiStuck Suicide Off!");
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("afk");
    }

    private EmbedCreateSpec.Builder defaultEmbedPopulate(final EmbedCreateSpec.Builder embedBuilder) {
        return embedBuilder
                .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                .addField("Walk Distance", "" + CONFIG.client.extra.antiafk.actions.walkDistance, false)
                .addField("Safe Walk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                .addField("Safe Gravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                .addField("Stuck Warning", (CONFIG.client.extra.antiafk.actions.stuckWarning ? "on" : "off") + " [Mention: " + (CONFIG.client.extra.antiafk.actions.stuckWarningMention ? "on" : "off") + "]", false)
                .addField("AntiStuck Suicide", (CONFIG.client.extra.antiafk.actions.antiStuck ? "on" : "off"), false)
                .color(Color.CYAN);
    }
}
