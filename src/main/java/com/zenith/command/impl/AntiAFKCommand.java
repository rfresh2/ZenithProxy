package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.AntiAFK;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;

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
                   "jump delay <int>"
            ),
            aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
            .then(argument("toggle", toggle()).executes(c -> {
                boolean toggle = getToggle(c, "toggle");
                CONFIG.client.extra.antiafk.enabled = toggle;
                MODULE_MANAGER.getModule(AntiAFK.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                    .title("AntiAFK " + (toggle ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("rotate")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.rotate = toggle;
                          c.getSource().getEmbedBuilder()
                              .title("Rotate " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotateDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbedBuilder()
                              .title("Rotate Delay Set!");
                          return 1;
                      }))))
            .then(literal("swing")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.swingHand = toggle;
                          c.getSource().getEmbedBuilder()
                              .title("Swing " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbedBuilder()
                              .title("Swing Delay Set!");
                          return 1;
                      }))))
            .then(literal("walk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.walk = toggle;
                          c.getSource().getEmbedBuilder()
                              .title("Walk " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walkDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbedBuilder()
                              .title("Walk Delay Set!");
                          return 1;
                      }))))
            .then(literal("safewalk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          boolean toggle = getToggle(c, "toggle");
                          CONFIG.client.extra.antiafk.actions.safeWalk = toggle;
                          c.getSource().getEmbedBuilder()
                              .title("SafeWalk " + (toggle ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("walkdistance")
                                .then(argument("walkdist", integer(1)).executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.walkDistance = IntegerArgumentType.getInteger(c, "walkdist");
                                    c.getSource().getEmbedBuilder()
                                        .title("Walk Distance Set!");
                                    return 1;
                                })))
                      .then(literal("jump")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    boolean toggle = getToggle(c, "toggle");
                                    CONFIG.client.extra.antiafk.actions.jump = toggle;
                                    c.getSource().getEmbedBuilder()
                                        .title("Jump " + (toggle ? "On!" : "Off!"));
                                    return 1;
                                }))
                                .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.jumpDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                                    c.getSource().getEmbedBuilder()
                                        .title("Jump Delay Set!");
                                    return 1;
                                })))));
    }

    @Override
    public List<String> aliases() {
        return asList("afk");
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder embedBuilder) {
        embedBuilder
            .addField("AntiAFK", toggleStr(CONFIG.client.extra.antiafk.enabled), false)
            .addField("Rotate", toggleStr(CONFIG.client.extra.antiafk.actions.rotate)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.rotateDelayTicks, false)
            .addField("Swing", toggleStr(CONFIG.client.extra.antiafk.actions.swingHand)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.swingDelayTicks, false)
            .addField("Walk", toggleStr(CONFIG.client.extra.antiafk.actions.walk)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.walkDelayTicks, false)
            .addField("Safe Walk", toggleStr(CONFIG.client.extra.antiafk.actions.safeWalk), false)
            .addField("Walk Distance", "" + CONFIG.client.extra.antiafk.actions.walkDistance, false)
            .addField("Jump", toggleStr(CONFIG.client.extra.antiafk.actions.jump)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.jumpDelayTicks, false)
            .color(Color.CYAN);
    }
}
