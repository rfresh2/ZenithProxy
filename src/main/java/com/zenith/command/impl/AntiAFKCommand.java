package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AntiAFK;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "antiAFK",
            CommandCategory.MODULE,
            "Configure the AntiAFK feature",
            asList("on/off",
                    "rotate on/off",
                    "rotate delay <int>",
                    "swing on/off",
                    "swing delay <int>",
                    "walk on/off",
                    "walk delay <int>",
                    "safeWalk on/off",
                    "walkDistance <int>",
                    "jump on/off",
                    "jump delay <int>",
                    "sneak on/off"
            ),
            asList("afk")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
            .then(argument("toggle", toggle()).executes(c -> {
                boolean toggle = getToggle(c, "toggle");
                CONFIG.client.extra.antiafk.enabled = toggle;
                MODULE_MANAGER.get(AntiAFK.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AntiAFK " + (toggle ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("rotate")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.rotate = toggle;
                          c.getSource().getEmbed()
                              .title("Rotate " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotateDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Rotate Delay Set!");
                          return 1;
                      }))))
            .then(literal("swing")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.swingHand = toggle;
                          c.getSource().getEmbed()
                              .title("Swing " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Swing Delay Set!");
                          return 1;
                      }))))
            .then(literal("walk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.walk = toggle;
                          c.getSource().getEmbed()
                              .title("Walk " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walkDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Walk Delay Set!");
                          return 1;
                      }))))
            .then(literal("safeWalk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.safeWalk = toggle;
                          c.getSource().getEmbed()
                              .title("SafeWalk " + (toggle ? "On!" : "Off!"));
                          return 1;
                      })))
            .then(literal("walkDistance")
                                .then(argument("walkdist", integer(1)).executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.walkDistance = IntegerArgumentType.getInteger(c, "walkdist");
                                    c.getSource().getEmbed()
                                        .title("Walk Distance Set!");
                                    return 1;
                                })))
            .then(literal("jump")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.jump = toggle;
                          c.getSource().getEmbed()
                              .title("Jump " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.jumpDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Jump Delay Set!");
                          return 1;
                      }))))
            .then(literal("sneak")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.sneak = toggle;
                          c.getSource().getEmbed()
                              .title("Sneak " + (toggle ? "On!" : "Off!"));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed embedBuilder) {
        embedBuilder
            .addField("AntiAFK", toggleStr(CONFIG.client.extra.antiafk.enabled), false)
            .addField("Rotate", toggleStr(CONFIG.client.extra.antiafk.actions.rotate)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.rotateDelayTicks, false)
            .addField("Swing", toggleStr(CONFIG.client.extra.antiafk.actions.swingHand)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.swingDelayTicks, false)
            .addField("Walk", toggleStr(CONFIG.client.extra.antiafk.actions.walk)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.walkDelayTicks, false)
            .addField("Safe Walk", toggleStr(CONFIG.client.extra.antiafk.actions.safeWalk), false)
            .addField("Walk Distance", CONFIG.client.extra.antiafk.actions.walkDistance, false)
            .addField("Jump", toggleStr(CONFIG.client.extra.antiafk.actions.jump)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.jumpDelayTicks, false)
            .addField("Sneak", toggleStr(CONFIG.client.extra.antiafk.actions.sneak), false)
            .color(Color.CYAN);
    }
}
