package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.impl.ActionLimiter;
import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.Collections;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ItemArgument.getItem;
import static com.zenith.command.brigadier.ItemArgument.item;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ActionLimiterCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("actionLimiter")
            .category(CommandCategory.MODULE)
            .description("""
                Limits player actions and movements.
                
                Players who login with the same account as the one used by ZenithProxy will be immune to these restrictions.
                
                If the movement limits are reached by a player, they will be disconnected while the proxy account will stay logged in.
                
                Other limits do not disconnect players and instead cancel the actions.
                """)
            .usageLines(
                "on/off",
                "allowMovement on/off",
                "movementDistance <distance>",
                "movementHome <x> <z>",
                "movementMinY <y>",
                "allowInventory on/off",
                "allowBlockBreaking on/off",
                "allowInteract on/off",
                "allowEnderChest on/off",
                "allowUseItem on/off",
                "allowBookSigning on/off",
                "allowChat on/off",
                "allowServerCommands on/off",
                "allowRespawn on/off",
                "itemsBlacklist on/off",
                "itemsBlacklist add/del <item>",
                "itemsBlacklist addAll <item 1>,<item 2>...",
                "itemsBlacklist clear",
                "itemsBlacklist list"
            )
            .aliases(
                "al"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("actionLimiter").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.enabled = getToggle(c, "toggle");
                MODULE.get(ActionLimiter.class).syncEnabledFromConfig();
            }))
            .then(literal("allowMovement").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowMovement = getToggle(c, "toggle");
            })))
            .then(literal("movementDistance").then(argument("distance", integer(0)).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementDistance = getInteger(c, "distance");
            })))
            .then(literal("movementHome").then(argument("x", integer()).then(argument("z", integer()).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementHomeX = getInteger(c, "x");
                CONFIG.client.extra.actionLimiter.movementHomeZ = getInteger(c, "z");
            }))))
            .then(literal("movementMinY").then(argument("y", integer(-64, 400)).executes(c -> {
                CONFIG.client.extra.actionLimiter.movementMinY = getInteger(c, "y");
            })))
            .then(literal("allowInventory").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowInventory = getToggle(c, "toggle");
            })))
            .then(literal("allowBlockBreaking").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowBlockBreaking = getToggle(c, "toggle");
            })))
            .then(literal("allowInteract").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowInteract = getToggle(c, "toggle");
            })))
            .then(literal("allowEnderChest").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowEnderChest = getToggle(c, "toggle");
            })))
            .then(literal("allowUseItem").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowUseItem = getToggle(c, "toggle");
            })))
            .then(literal("allowBookSigning").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowBookSigning = getToggle(c, "toggle");
            })))
            .then(literal("allowChat").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowChat = getToggle(c, "toggle");
            })))
            .then(literal("allowServerCommands").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowServerCommands = getToggle(c, "toggle");
            })))
            .then(literal("allowRespawn").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.actionLimiter.allowRespawn = getToggle(c, "toggle");
            })))
            .then(literal("itemsBlacklist")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.actionLimiter.itemsBlacklistEnabled = getToggle(c, "toggle");
                }))
                .then(literal("add").then(argument("item", item()).executes(c -> {
                    ItemData itemData = getItem(c, "item");
                    String id = itemData.name();
                    CONFIG.client.extra.actionLimiter.itemsBlacklist.add(id);
                    c.getSource().getEmbed()
                        .description(id + " added");
                })))
                .then(literal("addAll").then(argument("items", wordWithChars()).executes(c -> {
                    String itemsListStr = getString(c, "items");
                    String[] items = itemsListStr.split(",");
                    if (items.length == 0) {
                        c.getSource().getEmbed()
                            .title("No items provided");
                    }
                    for (String item : items) {
                        if (item.isBlank()) continue;
                        if (item.contains(":")) {
                            item = Key.key(item).value();
                        }
                        ItemData itemData = ItemRegistry.REGISTRY.get(item);
                        if (itemData == null) continue;
                        CONFIG.client.extra.actionLimiter.itemsBlacklist.add(itemData.name());
                    }
                    c.getSource().getEmbed()
                        .description("Items added");
                })))
                .then(literal("del").then(argument("item", item()).executes(c -> {
                    ItemData itemData = getItem(c, "item");
                    String id = itemData.name();
                    CONFIG.client.extra.actionLimiter.itemsBlacklist.remove(id);
                    c.getSource().getEmbed()
                        .description(id + " removed");
                })))
                .then(literal("clear").executes(c -> {
                    CONFIG.client.extra.actionLimiter.itemsBlacklist.clear();
                    c.getSource().getEmbed()
                        .description("Cleared");
                }))
                .then(literal("list").executes(c -> {
                    var items = new ArrayList<>(CONFIG.client.extra.actionLimiter.itemsBlacklist);
                    Collections.sort(items);
                    String list = "**Items Blacklist**:\n" + String.join("\n", items);
                    if (list.length() > 4000) {
                        list = list.substring(0, 4000) + "\n... (and more)";
                    }
                    c.getSource().getEmbed()
                        .description(list);
                })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .title("Action Limiter")
            .primaryColor();
        if (!builder.isDescriptionPresent()) {
            builder
                .addField("Action Limiter", toggleStr(CONFIG.client.extra.actionLimiter.enabled))
                .addField("Allow Movement", toggleStr(CONFIG.client.extra.actionLimiter.allowMovement))
                .addField("Movement Distance", String.valueOf(CONFIG.client.extra.actionLimiter.movementDistance))
                .addField("Movement Home", String.format("%d, %d", CONFIG.client.extra.actionLimiter.movementHomeX, CONFIG.client.extra.actionLimiter.movementHomeZ))
                .addField("Movement Min Y", String.valueOf(CONFIG.client.extra.actionLimiter.movementMinY))
                .addField("Allow Inventory", toggleStr(CONFIG.client.extra.actionLimiter.allowInventory))
                .addField("Allow Block Breaking", toggleStr(CONFIG.client.extra.actionLimiter.allowBlockBreaking))
                .addField("Allow Interact", toggleStr(CONFIG.client.extra.actionLimiter.allowInteract))
                .addField("Allow Ender Chest", toggleStr(CONFIG.client.extra.actionLimiter.allowEnderChest))
                .addField("Allow Use Item", toggleStr(CONFIG.client.extra.actionLimiter.allowUseItem))
                .addField("Allow Book Signing", toggleStr(CONFIG.client.extra.actionLimiter.allowBookSigning))
                .addField("Allow Chat", toggleStr(CONFIG.client.extra.actionLimiter.allowChat))
                .addField("Allow Server Commands", toggleStr(CONFIG.client.extra.actionLimiter.allowServerCommands))
                .addField("Allow Respawn", toggleStr(CONFIG.client.extra.actionLimiter.allowRespawn))
                .addField("Items Blacklist", toggleStr(CONFIG.client.extra.actionLimiter.itemsBlacklistEnabled));
        }
    }
}
