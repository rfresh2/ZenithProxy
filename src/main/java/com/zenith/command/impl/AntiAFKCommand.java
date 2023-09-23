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
import static java.util.Arrays.asList;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "antiAFK",
            "Configure the AntiAFK feature",
            asList("on/off",
                   "rotate on/off",
                   "swing on/off",
                   "walk on/off",
                   "safeWalk on/off",
                   "walkDistance <int>"
            ),
            aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
            .then(literal("on").executes(c -> {
                CONFIG.client.extra.antiafk.enabled = true;
                MODULE_MANAGER.getModule(AntiAFK.class).ifPresent(Module::syncEnabledFromConfig);
                defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                    .title("AntiAFK On!");
            }))
            .then(literal("off").executes(c -> {
                CONFIG.client.extra.antiafk.enabled = false;
                MODULE_MANAGER.getModule(AntiAFK.class).ifPresent(Module::syncEnabledFromConfig);
                defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                    .title("AntiAFK Off!");
            }))
            .then(literal("rotate")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotate = true;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Rotate On!");
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotate = false;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Rotate Off!");
                      })))
            .then(literal("swing")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingHand = true;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Swing On!");
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingHand = false;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Swing Off!");
                      })))
            .then(literal("walk")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walk = true;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Walk On!");
                      }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.walk = false;
                            defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                                .title("Walk Off!");
                        })))
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
            .then(literal("walkdistance")
                      .then(argument("walkdist", integer(1)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walkDistance = IntegerArgumentType.getInteger(c, "walkdist");
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder())
                              .title("Walk Distance Set!");
                          return 1;
                      })));
    }

    @Override
    public List<String> aliases() {
        return asList("afk");
    }

    private EmbedCreateSpec.Builder defaultEmbedPopulate(final EmbedCreateSpec.Builder embedBuilder) {
        return embedBuilder
            .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
            .addField("Rotate", CONFIG.client.extra.antiafk.actions.rotate ? "on" : "off", false)
            .addField("Swing", CONFIG.client.extra.antiafk.actions.swingHand ? "on" : "off", false)
            .addField("Walk", CONFIG.client.extra.antiafk.actions.walk ? "on" : "off", false)
            .addField("Safe Walk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
            .addField("Walk Distance", "" + CONFIG.client.extra.antiafk.actions.walkDistance, false)
            .color(Color.CYAN);
    }
}
