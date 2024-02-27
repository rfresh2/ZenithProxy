package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoEat;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoEatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoEat",
                                 CommandCategory.MODULE,
                                 "Configures the AutoEat feature",
                                 asList("on/off", "health <int>", "hunger <int>", "warning on/off"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoEat")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoEat.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.get(AutoEat.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoEat " + toggleStrCaps(CONFIG.client.extra.autoEat.enabled));
                return 1;
            }))
            .then(literal("health")
                      .then(argument("health", integer(1, 19)).executes(c -> {
                          int health = IntegerArgumentType.getInteger(c, "health");
                          CONFIG.client.extra.autoEat.healthThreshold = health;
                          c.getSource().getEmbed()
                              .title("AutoEat Health Threshold Set")
                              .color(Color.CYAN)
                              .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold, false)
                              .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold, false)
                              .addField("Warning", Boolean.toString(CONFIG.client.extra.autoEat.warning), false);
                          return 1;
                      })))
            .then(literal("hunger")
                      .then(argument("hunger", integer(1, 19)).executes(c -> {
                          int hunger = IntegerArgumentType.getInteger(c, "hunger");
                          CONFIG.client.extra.autoEat.hungerThreshold = hunger;
                          c.getSource().getEmbed()
                              .title("AutoEat Hunger Threshold Set")
                              .color(Color.CYAN)
                              .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold, false)
                              .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold, false)
                              .addField("Warning", Boolean.toString(CONFIG.client.extra.autoEat.warning), false);
                          return 1;
                      })))
            .then(literal("warning")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.autoEat.warning = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("AutoEat Warning " + toggleStrCaps(CONFIG.client.extra.autoEat.warning));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled), false)
            .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold, false)
            .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold, false)
            .addField("Warning", Boolean.toString(CONFIG.client.extra.autoEat.warning), false)
            .color(Color.CYAN);
    }
}
