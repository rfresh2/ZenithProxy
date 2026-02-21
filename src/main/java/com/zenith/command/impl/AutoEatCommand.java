package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.mc.food.FoodRegistry;
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
                "allowUnsafeFood on/off",
                "blacklist add <food_name>",
                "blacklist remove <food_name>",
                "blacklist list",
                "blacklist clear"
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
            .then(literal("health").then(argument("health", integer(-1)).executes(c -> {
                CONFIG.client.extra.autoEat.healthThreshold = IntegerArgumentType.getInteger(c, "health");
                c.getSource().getEmbed()
                    .title("AutoEat Health Threshold Set");
                return OK;
            })))
            .then(literal("hunger").then(argument("hunger", integer(-1)).executes(c -> {
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
            })))
            .then(literal("blacklist")
                .then(literal("add").then(argument("food", StringArgumentType.word()).executes(c -> {
                    String food = StringArgumentType.getString(c, "food");
                    if (FoodRegistry.REGISTRY.get(food) == null) {
                        c.getSource().getEmbed()
                            .title("AutoEat Blacklist: Unknown food \"" + food + "\"")
                            .errorColor();
                        return OK;
                    }
                    if (!CONFIG.client.extra.autoEat.blacklist.contains(food))
                        CONFIG.client.extra.autoEat.blacklist.add(food);
                    c.getSource().getEmbed()
                        .title("AutoEat Blacklist: Added \"" + food + "\"");
                    return OK;
                })))
                .then(literal("remove").then(argument("food", StringArgumentType.word()).executes(c -> {
                    String food = StringArgumentType.getString(c, "food");
                    CONFIG.client.extra.autoEat.blacklist.remove(food);
                    c.getSource().getEmbed()
                        .title("AutoEat Blacklist: Removed \"" + food + "\"");
                    return OK;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbed()
                        .title("AutoEat Blacklist")
                        .addField("Foods", CONFIG.client.extra.autoEat.blacklist.isEmpty()
                            ? "Empty"
                            : String.join(", ", CONFIG.client.extra.autoEat.blacklist));
                    return OK;
                }))
                .then(literal("clear").executes(c -> {
                    CONFIG.client.extra.autoEat.blacklist.clear();
                    c.getSource().getEmbed().title("AutoEat Blacklist Cleared");
                    return OK;
                }))
            );
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled))
            .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold)
            .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold)
            .addField("Warning", toggleStr(CONFIG.client.extra.autoEat.warning))
            .addField("Allow Unsafe Food", toggleStr(CONFIG.client.extra.autoEat.allowUnsafeFood))
            .addField("Blacklist", CONFIG.client.extra.autoEat.blacklist.isEmpty()
                ? "Empty"
                : String.join(", ", CONFIG.client.extra.autoEat.blacklist))
            .primaryColor();
    }
}
