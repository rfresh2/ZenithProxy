package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.config.Config;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.saveConfigAsync;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoDropCommand extends Command {

    public static final Config.Client.AutoDrop AUTO_DROP_CONFIG = CONFIG.client.extra.autoDrop;

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("autodrop")
                .category(CommandCategory.MODULE)
                .description("""
                        Automatically drop specified items from player inventory.
                        """)
                .usageLines(
                        "",
                        "toggle <on/off>",
                        "mode <whitelist/blacklist>",
                        "add <item>",
                        "remove <item>",
                        "list",
                        "clear",
                        "delay <ticks>"
                )
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autodrop")
                .executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    printStatus(c.getSource().getEmbed());
                    return OK;
                })
                .then(literal("toggle").then(argument("toggle", toggle()).executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    AUTO_DROP_CONFIG.enabled = getToggle(c, "toggle");
                    saveConfigAsync();
                    settingsEmbed(c.getSource().getEmbed(), "AutoDrop " + (AUTO_DROP_CONFIG.enabled ? "Enabled" : "Disabled"));
                    return OK;
                })))
                .then(literal("mode").then(argument("mode", string()).executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    String mode = getString(c, "mode").toLowerCase();
                    if (mode.equals("whitelist")) {
                        AUTO_DROP_CONFIG.whitelistMode = true;
                    } else if (mode.equals("blacklist")) {
                        AUTO_DROP_CONFIG.whitelistMode = false;
                    } else {
                        c.getSource().getEmbed()
                                .title("Error")
                                .description("Invalid mode. Use 'whitelist' or 'blacklist'")
                                .errorColor();
                        return ERROR;
                    }
                    saveConfigAsync();
                    settingsEmbed(c.getSource().getEmbed(), "Mode set to: " + mode);
                    return OK;
                })))
                .then(literal("add").then(argument("item", string()).executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    String item = getString(c, "item");
                    if (!isValidItem(item)) {
                        c.getSource().getEmbed()
                                .title("Error")
                                .description("Invalid item: " + item)
                                .errorColor();
                        return ERROR;
                    }
                    if (AUTO_DROP_CONFIG.items.contains(item)) {
                        c.getSource().getEmbed()
                                .title("Error")
                                .description("Item already in list: " + item)
                                .errorColor();
                        return ERROR;
                    }
                    AUTO_DROP_CONFIG.items.add(item);
                    saveConfigAsync();
                    settingsEmbed(c.getSource().getEmbed(), "Added item: " + item);
                    return OK;
                })))
                .then(literal("remove").then(argument("item", string()).executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    String item = getString(c, "item");
                    if (!AUTO_DROP_CONFIG.items.contains(item)) {
                        c.getSource().getEmbed()
                                .title("Error")
                                .description("Item not in list: " + item)
                                .errorColor();
                        return ERROR;
                    }
                    AUTO_DROP_CONFIG.items.remove(item);
                    saveConfigAsync();
                    settingsEmbed(c.getSource().getEmbed(), "Removed item: " + item);
                    return OK;
                })))
                .then(literal("list").executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    printItemList(c.getSource().getEmbed());
                    return OK;
                }))
                .then(literal("clear").executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    AUTO_DROP_CONFIG.items.clear();
                    saveConfigAsync();
                    settingsEmbed(c.getSource().getEmbed(), "Item list cleared");
                    return OK;
                }))
                .then(literal("delay").then(argument("ticks", string()).executes(c -> {
                    if (!verifyLoggedIn(c.getSource().getEmbed())) return ERROR;
                    try {
                        int delay = Integer.parseInt(getString(c, "ticks"));
                        if (delay < 1 || delay > 1200) {
                            c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Delay must be between 1 and 1200 ticks")
                                    .errorColor();
                            return ERROR;
                        }
                        AUTO_DROP_CONFIG.delayBetweenDrops = delay;
                        saveConfigAsync();
                        settingsEmbed(c.getSource().getEmbed(), "Delay set to: " + delay + " ticks");
                        return OK;
                    } catch (NumberFormatException e) {
                        c.getSource().getEmbed()
                                .title("Error")
                                .description("Invalid number format")
                                .errorColor();
                        return ERROR;
                    }
                })));
    }

    private void printStatus(Embed embed) {
        embed.title("AutoDrop Status")
                .addField("Enabled", AUTO_DROP_CONFIG.enabled)
                .addField("Mode", AUTO_DROP_CONFIG.whitelistMode ? "Whitelist" : "Blacklist")
                .addField("Delay", AUTO_DROP_CONFIG.delayBetweenDrops + " ticks")
                .addField("Items Count", AUTO_DROP_CONFIG.items.size())
                .primaryColor();
    }

    private void printItemList(Embed embed) {
        embed.title("AutoDrop Item List (" + (AUTO_DROP_CONFIG.whitelistMode ? "Whitelist" : "Blacklist") + ")");
        for (String item : AUTO_DROP_CONFIG.items) {
            embed.addField(item, isValidItem(item) ? "✓ Valid" : "✗ Invalid", false);
        }
        embed.primaryColor();
    }

    private boolean isValidItem(String itemName) {
        return ItemRegistry.REGISTRY.get(itemName) != null;
    }

    private void settingsEmbed(Embed embed, String message) {
        embed.title("AutoDrop Settings")
                .description(message)
                .addField("Enabled", AUTO_DROP_CONFIG.enabled)
                .addField("Mode", AUTO_DROP_CONFIG.whitelistMode ? "Whitelist" : "Blacklist")
                .addField("Delay", AUTO_DROP_CONFIG.delayBetweenDrops + " ticks")
                .addField("Items Count", AUTO_DROP_CONFIG.items.size())
                .primaryColor();
    }

    private boolean verifyLoggedIn(Embed embed) {
        var client = Proxy.getInstance().getClient();
        if (client == null || !Proxy.getInstance().isConnected()) {
            embed.title("Error")
                    .description("Not logged in!")
                    .errorColor();
            return false;
        }
        return true;
    }
}
