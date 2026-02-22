package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.mc.food.FoodRegistry;
import com.zenith.module.impl.AutoEat;
import com.zenith.util.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.FoodArgument.food;
import static com.zenith.command.brigadier.FoodArgument.getFood;
import static com.zenith.command.brigadier.ItemArgument.item;
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

             Which foods to eat can be configured based on modes:

                * `all`: any food
                * `whitelist`: only added foods
                * `blacklist`: any food not added
             """)
            .usageLines(
                "on/off",
                "health <int>",
                "hunger <int>",
                "warning on/off",
                "allowUnsafeFood on/off",
                "mode <all/whitelist/blacklist>",
                "add/del <food>",
                "addAll <food1>,<food2>,...",
                "list",
                "clear"
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
            .then(literal("mode").then(argument("mode", enumStrings("all", "whitelist", "blacklist")).executes(c -> {
                var modeString = getString(c, "mode").toUpperCase();
                CONFIG.client.extra.autoEat.mode = Config.Client.Extra.AutoEat.Mode.valueOf(modeString);
                c.getSource().getEmbed()
                    .title("Mode Set");
            })))
            .then(literal("add").then(argument("food", food()).executes(c -> {
                var food = getFood(c, "food");
                CONFIG.client.extra.autoEat.foods.add(food.name());
                CONFIG.client.extra.autoEat.foods.removeIf(i -> FoodRegistry.REGISTRY.get(i) == null);
                c.getSource().getEmbed()
                    .title("Food Added")
                    .description(foodList());
            })))
            .then(literal("addAll").then(argument("foods", StringArgumentType.greedyString()).executes(c -> {
                var foodList = getString(c, "foods").split(",");
                List<String> invalidFoods = new ArrayList<>();
                for (var food : foodList) {
                    var foodData = FoodRegistry.REGISTRY.get(food);
                    if (foodData == null) {
                        invalidFoods.add(food);
                        continue;
                    }
                    CONFIG.client.extra.autoEat.foods.add(foodData.name());
                }
                c.getSource().getEmbed()
                    .title("Foods Added")
                    .addField("Added Foods Count", foodList.length - invalidFoods.size())
                    .description(foodList());
                if (!invalidFoods.isEmpty()) {
                    c.getSource().getEmbed()
                        .addField("Invalid Foods", invalidFoods.stream().map(s -> "`" + s + "`").reduce((a, b) -> a + ", " + b).orElse(""));
                }
            })))
            .then(literal("del").then(argument("food", item()).executes(c -> {
                var food = getFood(c, "food");
                CONFIG.client.extra.autoEat.foods.remove(food.name());
                CONFIG.client.extra.autoEat.foods.removeIf(i -> FoodRegistry.REGISTRY.get(i) == null);
                c.getSource().getEmbed()
                    .title("Food Deleted")
                    .description(foodList());
            })))
            .then(literal("list").executes(c -> {
                CONFIG.client.extra.autoEat.foods.removeIf(i -> FoodRegistry.REGISTRY.get(i) == null);
                c.getSource().getEmbed()
                    .title("Food Blacklist")
                    .description(foodList());
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.autoEat.foods.clear();
                c.getSource().getEmbed()
                    .title("Food Blacklist Cleared")
                    .description(foodList());
            }));
    }

    @Override
    public void defaultHandler(final CommandContext context) {
        if (context.getData().containsKey("noDefaultEmbed")) return;
        context.getEmbed()
            .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled))
            .addField("Mode", CONFIG.client.extra.autoEat.mode.name().toLowerCase())
            .addField("Health Threshold", CONFIG.client.extra.autoEat.healthThreshold)
            .addField("Hunger Threshold", CONFIG.client.extra.autoEat.hungerThreshold)
            .addField("Warning", toggleStr(CONFIG.client.extra.autoEat.warning))
            .addField("Allow Unsafe Food", toggleStr(CONFIG.client.extra.autoEat.allowUnsafeFood))
            .primaryColor();
    }

    String foodList() {
        var items = new ArrayList<>(CONFIG.client.extra.autoEat.foods);
        Collections.sort(items);
        String list = "**Food List**:\n" + String.join("\n", items);
        if (list.length() > 4000) {
            list = list.substring(0, 4000) + "\n... (and more)";
        }
        return list;
    }
}
