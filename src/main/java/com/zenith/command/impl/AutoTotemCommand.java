package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
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
                                     "health <int>",
                                     "alert on/off",
                                     "alert mention on/off"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoTotem")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoTotem.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.get(AutoTotem.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoTotem " + toggleStrCaps(CONFIG.client.extra.autoTotem.enabled));
                return 1;
            }))
            .then(literal("health")
                      .then(argument("healthArg", integer(0, 20)).executes(c -> {
                          CONFIG.client.extra.autoTotem.healthThreshold = c.getArgument("healthArg", Integer.class);
                          c.getSource().getEmbed()
                              .title("Auto Totem Health Threshold Set!");
                          return 1;
                      })))
            .then(literal("alert")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.autoTotem.totemPopAlert = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Auto Totem Alert " + toggleStrCaps(CONFIG.client.extra.autoTotem.totemPopAlert));
                          return 1;
                      }))
                      .then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.autoTotem.totemPopAlertMention = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Auto Totem Mention " + toggleStrCaps(CONFIG.client.extra.autoTotem.totemPopAlertMention));
                          return 1;
                      }))));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Auto Totem", toggleStr(CONFIG.client.extra.autoTotem.enabled), false)
            .addField("Health Threshold", CONFIG.client.extra.autoTotem.healthThreshold, true)
            .addField("Alert", toggleStr(CONFIG.client.extra.autoTotem.totemPopAlert), false)
            .addField("Alert Mention", toggleStr(CONFIG.client.extra.autoTotem.totemPopAlertMention), true)
            .color(Color.CYAN);
    }
}
