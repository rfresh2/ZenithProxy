package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.impl.AutoDrop;
import com.zenith.util.config.Config;
import com.zenith.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ItemArgument.getItem;
import static com.zenith.command.brigadier.ItemArgument.item;
import static com.zenith.command.brigadier.RotationArgument.getRotation;
import static com.zenith.command.brigadier.RotationArgument.rotation;
import static com.zenith.command.brigadier.TimeArgument.time;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoDropCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("autoDrop")
                .category(CommandCategory.MODULE)
                .description("""
                    Automatically drop items in player inventory.

                    Dropping can be configured based on modes:

                    * `all`: any item
                    * `whitelist`: only added items
                    * `blacklist`: any item not added
                    """)
                .usageLines(
                    "on/off",
                    "mode <all/whitelist/blacklist>",
                    "add/del <item>",
                    "addAll <item1>,<item2>,...",
                    "list",
                    "clear",
                    "delay <ticks>",
                    "dropStack on/off",
                    "rotation on/off",
                    "rotation sync",
                    "rotation <yaw> <pitch>"
                )
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoDrop")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoDrop.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoDrop " + toggleStrCaps(CONFIG.client.extra.autoDrop.enabled));
                MODULE.get(AutoDrop.class).syncEnabledFromConfig();
            }))
            .then(literal("mode").then(argument("mode", enumStrings(Config.Client.AutoDrop.Mode.values())).executes(c -> {
                var modeString = getString(c, "mode").toUpperCase();
                CONFIG.client.extra.autoDrop.mode = Config.Client.AutoDrop.Mode.valueOf(modeString);
                c.getSource().getEmbed()
                    .title("Mode Set");
            })))
            .then(literal("add").then(argument("item", item()).executes(c -> {
                var item = getItem(c, "item");
                var itemName = item.name();
                if (!CONFIG.client.extra.autoDrop.items.contains(itemName)) {
                    CONFIG.client.extra.autoDrop.items.add(itemName);
                }
                Collections.sort(CONFIG.client.extra.autoDrop.items);
                c.getSource().getEmbed()
                    .title("Item Added")
                    .description(itemsListToString());
            })))
            .then(literal("del").then(argument("item", item()).executes(c -> {
                var item = getItem(c, "item");
                var itemName = item.name();
                CONFIG.client.extra.autoDrop.items.removeIf(i -> i.equals(itemName));
                CONFIG.client.extra.autoDrop.items.removeIf(i -> ItemRegistry.REGISTRY.get(i) == null);
                c.getSource().getEmbed()
                    .title("Item Removed")
                    .description(itemsListToString());
            })))
            .then(literal("addAll").then(argument("items", wordWithChars()).executes(c -> {
                var itemsList = getString(c, "items").split(",");
                List<String> invalidItems = new ArrayList<>();
                for (var item : itemsList) {
                    var itemData = ItemRegistry.REGISTRY.get(item);
                    if (itemData == null) {
                        invalidItems.add(item);
                        continue;
                    }
                    if (CONFIG.client.extra.autoDrop.items.contains(itemData.name())) continue;
                    CONFIG.client.extra.autoDrop.items.add(itemData.name());
                }
                Collections.sort(CONFIG.client.extra.autoDrop.items);
                c.getSource().getEmbed()
                    .title("Items Added")
                    .addField("Added Items Count", itemsList.length - invalidItems.size())
                    .description(itemsListToString());
                if (!invalidItems.isEmpty()) {
                    c.getSource().getEmbed()
                        .addField("Invalid Items", String.join(", ", invalidItems));
                }
            })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Items List")
                    .description(itemsListToString());
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.autoDrop.items.clear();
                c.getSource().getEmbed()
                    .title("Items List Cleared")
                    .description(itemsListToString());
            }))
            .then(literal("delay").then(argument("ticks", time(0)).executes(c -> {
                CONFIG.client.extra.autoDrop.delayTicks = getInteger(c, "ticks");
                c.getSource().getEmbed()
                    .title("Delay Set");
            })))
            .then(literal("dropStack").then(argument("toggle", toggle()).executes(c -> {;
                CONFIG.client.extra.autoDrop.dropStack = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Drop Stack " + toggleStrCaps(CONFIG.client.extra.autoDrop.dropStack));
            })))
            .then(literal("rotation")
                      .then(argument("toggle", toggle()).executes(c -> {;
                              CONFIG.client.extra.autoDrop.requiresRotation = getToggle(c, "toggle");
                              c.getSource().getEmbed()
                                  .title("Rotation " + toggleStrCaps(CONFIG.client.extra.autoDrop.requiresRotation));
                      }))
                      .then(literal("sync").executes(c -> {
                            // normalize yaw and pitch to -180 to 180 and -90 to 90
                            CONFIG.client.extra.autoDrop.yaw = MathHelper.wrapYaw(CACHE.getPlayerCache().getYaw());
                            CONFIG.client.extra.autoDrop.pitch = MathHelper.wrapPitch(CACHE.getPlayerCache().getPitch());
                            c.getSource().getEmbed()
                                .title("Rotation Set");
                      }))
                      .then(argument("rotation", rotation()).executes(c -> {
                            var rotation = getRotation(c, "rotation");
                            CONFIG.client.extra.autoDrop.yaw = (float) rotation.getX();
                            CONFIG.client.extra.autoDrop.pitch = (float) rotation.getY();
                            c.getSource().getEmbed()
                                .title("Rotation Set");
                        })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("AutoDrop", toggleStr(CONFIG.client.extra.autoDrop.enabled))
            .addField("Mode", CONFIG.client.extra.autoDrop.mode.name().toLowerCase())
            .addField("Delay Ticks", CONFIG.client.extra.autoDrop.delayTicks)
            .addField("Drop Stack", toggleStr(CONFIG.client.extra.autoDrop.dropStack))
            .addField("Rotation", toggleStr(CONFIG.client.extra.autoDrop.requiresRotation))
            .addField("Yaw", CONFIG.client.extra.autoDrop.yaw)
            .addField("Pitch", CONFIG.client.extra.autoDrop.pitch)
            .primaryColor();
    }

    private String itemsListToString() {
        var items = CONFIG.client.extra.autoDrop.items;
        var sb = new StringBuilder();
        sb.append("**Items List**\n\n");
        if (items.isEmpty()) return "None!";
        for (int i = 0; i < items.size(); i++) {
            var itemName = items.get(i);
            sb.append("`");
            sb.append(itemName);
            sb.append("`\n");
        }
        return sb.toString();
    }
}
