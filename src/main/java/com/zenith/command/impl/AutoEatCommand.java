package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoEat;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoEatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoEat")
            .category(CommandCategory.MODULE)
            .description("""
             Automatically eats food when health or hunger is below a set threshold.
             """)
            .usageLines(
                "on/off",
                "health <int>",
                "hunger <int>",
                "warning on/off",
                "allowUnsafeFood on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoEat")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoEat.enabled = getToggle(c, "toggle");
                MODULE.get(AutoEat.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoEat " + toggleStrCaps(CONFIG.client.extra.autoEat.enabled));
                return OK;
            }))
            .then(literal("health").then(argument("health", integer(0)).executes(c -> {
                CONFIG.client.extra.autoEat.healthThreshold = IntegerArgumentType.getInteger(c, "health");
                c.getSource().getEmbed()
                    .title("AutoEat Health Threshold Set");
                return OK;
            })))
            .then(literal("hunger").then(argument("hunger", integer(0)).executes(c -> {
                CONFIG.client.extra.autoEat.hungerThreshold = IntegerArgumentType.getInteger(c, "hunger");
                c.getSource().getEmbed()
                    .title("AutoEat Hunger Threshold Set");
                return OK;
            })))
            .then(literal("warning").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoEat.warning = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoEat Warning " + toggleStrCaps(CONFIG.client.extra.autoEat.warning));
                return OK;
            })))
            .then(literal("allowUnsafeFood").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoEat.allowUnsafeFood = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoEat Allow Unsafe Food " + toggleStrCaps(CONFIG.client.extra.autoEat.allowUnsafeFood));
            })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled))
            .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold)
            .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold)
            .addField("Warning", toggleStr(CONFIG.client.extra.autoEat.warning))
            .addField("Allow Unsafe Food", toggleStr(CONFIG.client.extra.autoEat.allowUnsafeFood))
            .primaryColor();
    }
}
