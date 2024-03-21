package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AntiLeak;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AntiLeakCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("antiLeak",
                                 CommandCategory.MODULE,
                                 "Configures the AntiLeak module",
                                 asList(
                                     "on/off",
                                     "rangeCheck on/off",
                                     "rangeFactor <number>"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiLeak")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.antiLeak.enabled = getToggle(c, "toggle");
                MODULE.get(AntiLeak.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AntiLeak " + toggleStrCaps(CONFIG.client.extra.antiLeak.enabled));
                return 1;
            }))
            .then(literal("rangeCheck")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiLeak.rangeCheck = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("RangeCheck " + toggleStrCaps(CONFIG.client.extra.antiLeak.rangeCheck));
                          return 1;
                      })))
            .then(literal("rangeFactor")
                      .then(argument("factor", doubleArg(0.1, 1000.0)).executes(c -> {
                          CONFIG.client.extra.antiLeak.rangeFactor = getDouble(c, "factor");
                          c.getSource().getEmbed()
                              .title("RangeFactor set to " + CONFIG.client.extra.antiLeak.rangeFactor);
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AntiLeak", toggleStr(CONFIG.client.extra.antiLeak.enabled), false)
            .addField("RangeCheck", toggleStr(CONFIG.client.extra.antiLeak.rangeCheck), false)
            .addField("RangeCheck Factor", String.valueOf(CONFIG.client.extra.antiLeak.rangeFactor), false)
            .color(Color.CYAN);
    }
}
