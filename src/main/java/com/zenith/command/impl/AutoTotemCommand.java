package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.Module;
import com.zenith.module.impl.AutoTotem;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoTotemCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoTotem",
                                 CommandCategory.MODULE,
                                 "Automatically equips totems in the offhand",
                                 asList(
                                     "on/off",
                                     "health <int>"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoTotem")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoTotem.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(AutoTotem.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbed()
                    .title("AutoTotem " + (CONFIG.client.extra.autoTotem.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("health")
                      .then(argument("healthArg", integer(0, 20)).executes(c -> {
                          CONFIG.client.extra.autoTotem.healthThreshold = c.getArgument("healthArg", Integer.class);
                          c.getSource().getEmbed()
                              .title("Auto Totem Health Threshold Set!");
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Auto Totem", toggleStr(CONFIG.client.extra.autoTotem.enabled), false)
            .addField("Health Threshold", CONFIG.client.extra.autoTotem.healthThreshold, true)
            .color(Color.CYAN);
    }
}
