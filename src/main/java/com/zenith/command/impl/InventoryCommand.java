package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.*;
import com.zenith.feature.inventory.util.InventoryActionMacros;
import com.zenith.mc.item.ContainerTypeInfoRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.RequestFuture;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.TimeArgument.time;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class InventoryCommand extends Command {
    private static final int INV_ACTION_PRIORITY = 1000000;

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("inventory")
            .category(CommandCategory.INFO)
            .description("""
                Show and interact with the player's inventory or containers.
                """)
            .usageLines(
                "",
                "show",
                "hold <slot>",
                "swap <from> <to>",
                "drop <slot>",
                "drop stack <slot>",
                "close",
                "withdraw",
                "deposit",
                "click <left/right> <slot>",
                "button <buttonId>",
                "actionDelayTicks <ticks>",
                "ncpStrict on/off",
                "autoCloseOpenContainers on/off",
                "autoCloseOpenContainers delaySeconds <seconds>"
            )
            .aliases("inv")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("inventory")
            .executes(c -> {
                if (!verifyLoggedIn(c.getSource().getEmbed())) return;
                printInvAscii(c.getSource().getMultiLineOutput(), true);
            })
            .then(literal("show").executes(c -> {
                if (!verifyLoggedIn(c.getSource().getEmbed())) return;
                printInvAscii(c.getSource().getMultiLineOutput(), false);
            }))
            .then(literal("hold").then(argument("slot", integer(36, 200)).executes(c -> {
                if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                var slot = getInteger(c, "slot");
                int hotbarSlot = getHotbarSlot0();
                int hotbarIndex = slot - hotbarSlot;
                if (hotbarIndex < 0 || hotbarIndex > 8) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("Slot: " + slot + " is not a hotbar slot")
                        .errorColor();
                    return OK;
                }
                var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                        .owner(this)
                        .actions(new SetHeldItem(hotbarIndex))
                        .priority(INV_ACTION_PRIORITY)
                        .build())
                    .get();
                if (accepted) {
                    logInv();
                    c.getSource().setNoOutput(true);
                } else {
                    c.getSource().getEmbed()
                        .title("Failed")
                        .description("Another inventory action has taken priority this tick, try again")
                        .errorColor();
                }
                return OK;
            })))
            .then(literal("swap")
                .then(argument("from", integer(0, 200)).then(argument("to", integer(0, 200)).executes(c -> {
                    if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                    var from = getInteger(c, "from");
                    var to = getInteger(c, "to");
                    var containerSize = CACHE.getPlayerCache().getInventoryCache().getOpenContainer().getSize();
                    if (from >= containerSize) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("From slot: " + from + " is out of bounds")
                            .errorColor();
                        return OK;
                    }
                    if (to >= containerSize) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("To slot: " + to + " is out of bounds")
                            .errorColor();
                        return OK;
                    }
                    var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .actions(InventoryActionMacros.swapSlots(getOpenContainerId(), from, to))
                            .priority(INV_ACTION_PRIORITY)
                            .build())
                        .get();
                    if (accepted) {
                        logInv();
                        c.getSource().setNoOutput(true);
                    } else {
                        c.getSource().getEmbed()
                            .title("Failed")
                            .description("Another inventory action has taken priority this tick, try again")
                            .errorColor();
                    }
                    return OK;
                }))))
            .then(literal("drop")
                .then(literal("stack")
                    .then(argument("slot", integer(0, 200)).executes(c -> {
                        if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                        var slot = getInteger(c, "slot");
                        Container container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
                        var containerSize = container.getSize();
                        if (slot >= containerSize) {
                            c.getSource().getEmbed()
                                .title("Error")
                                .description("Slot: " + slot + " is out of bounds")
                                .errorColor();
                            return OK;
                        }
                        var stack = container.getItemStack(slot);
                        if (stack == Container.EMPTY_STACK) {
                            c.getSource().getEmbed()
                                .title("Error")
                                .description("Slot: " + slot + " is empty")
                                .errorColor();
                            return OK;
                        }
                        var accepted = drop(slot, true).get();
                        if (accepted) {
                            logInv();
                            c.getSource().setNoOutput(true);
                        } else {
                            c.getSource().getEmbed()
                                .title("Failed")
                                .description("Another inventory action has taken priority this tick, try again")
                                .errorColor();
                        }
                        return OK;
                    })))
                .then(argument("slot", integer(0, 200)).executes(c -> {
                    if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                    var slot = getInteger( c, "slot");
                    Container container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
                    var containerSize = container.getSize();
                    if (slot >= containerSize) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Slot: " + slot + " is out of bounds")
                            .errorColor();
                        return OK;
                    }
                    var stack = container.getItemStack(slot);
                    if (stack == Container.EMPTY_STACK) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Slot: " + slot + " is empty")
                            .errorColor();
                        return OK;
                    }
                    var accepted = drop(slot, false).get();
                    if (accepted) {
                        logInv();
                        c.getSource().setNoOutput(true);
                    } else {
                        c.getSource().getEmbed()
                            .title("Failed")
                            .description("Another inventory action has taken priority this tick, try again")
                            .errorColor();
                    }
                    return OK;
                })))
            .then(literal("close").executes(c -> {
                if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                var openContainerId = getOpenContainerId();
                if (openContainerId == 0) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("No open container to close")
                        .errorColor();
                    return OK;
                }
                var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                        .owner(this)
                        .actions(new CloseContainer(openContainerId))
                        .priority(INV_ACTION_PRIORITY)
                        .build())
                    .get();
                if (accepted) {
                    logInv();
                    c.getSource().setNoOutput(true);
                } else {
                    c.getSource().getEmbed()
                        .title("Failed")
                        .description("Another inventory action has taken priority this tick, try again")
                        .errorColor();
                }
                return OK;
            }))
            .then(literal("withdraw").executes(c -> {
                if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                if (getOpenContainerId() == 0) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("No open container to withdraw from. Use `b click right <x> <y> <z>` to open a container")
                        .errorColor();
                    return OK;
                }
                var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                        .owner(this)
                        .actions(InventoryActionMacros.withdraw(getOpenContainerId()))
                        .priority(INV_ACTION_PRIORITY)
                        .build())
                    .get();
                if (accepted) {
                    logInv();
                    c.getSource().setNoOutput(true);
                } else {
                    c.getSource().getEmbed()
                        .title("Failed")
                        .description("Another inventory action has taken priority this tick, try again")
                        .errorColor();
                }
                return OK;
            }))
            .then(literal("deposit").executes(c -> {
                if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                if (getOpenContainerId() == 0) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("No open container to deposit to. Use `b click right <x> <y> <z>` to open a container")
                        .errorColor();
                    return OK;
                }
                var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                        .owner(this)
                        .actions(InventoryActionMacros.deposit(getOpenContainerId()))
                        .priority(INV_ACTION_PRIORITY)
                        .build())
                    .get();
                if (accepted) {
                    logInv();
                    c.getSource().setNoOutput(true);
                } else {
                    c.getSource().getEmbed()
                        .title("Failed")
                        .description("Another inventory action has taken priority this tick, try again")
                        .errorColor();
                }
                return OK;
            }))
            .then(literal("click")
                .then(literal("left").then(argument("slot", integer(0, 100)).executes(c  -> {
                    if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                    int slot = getInteger(c, "slot");
                    var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
                    var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .actions(new ClickItem(container.getContainerId(), slot, ClickItemAction.LEFT_CLICK))
                            .priority(INV_ACTION_PRIORITY)
                            .build())
                        .get();
                    if (accepted) {
                        logInv();
                        c.getSource().setNoOutput(true);
                    } else {
                        c.getSource().getEmbed()
                            .title("Failed")
                            .description("Another inventory action has taken priority this tick, try again")
                            .errorColor();
                    }
                    return OK;
                })))
                .then(literal("right").then(argument("slot", integer(0, 100)).executes(c -> {
                    if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                    int slot = getInteger(c, "slot");
                    var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
                    var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .actions(new ClickItem(container.getContainerId(), slot, ClickItemAction.RIGHT_CLICK))
                            .priority(INV_ACTION_PRIORITY)
                            .build())
                        .get();
                    if (accepted) {
                        logInv();
                        c.getSource().setNoOutput(true);
                    } else {
                        c.getSource().getEmbed()
                            .title("Failed")
                            .description("Another inventory action has taken priority this tick, try again")
                            .errorColor();
                    }
                    return OK;
                }))))
            .then(literal("button").then(argument("buttonId", integer(0, 1000)).executes(c -> {
                if (!verifyAbleToDoInvActions(c.getSource().getEmbed())) return OK;
                if (getOpenContainerId() == 0) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("No open container to click button in. Use `b click right <x> <y> <z>` to open a container")
                        .errorColor();
                    return OK;
                }
                int buttonId = getInteger(c, "buttonId");
                var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
                var accepted = INVENTORY.submit(InventoryActionRequest.builder()
                        .owner(this)
                        .actions(new ContainerButtonClick(container.getContainerId(), buttonId))
                        .priority(INV_ACTION_PRIORITY)
                        .build())
                    .get();
                if (accepted) {
                    logInv();
                    c.getSource().setNoOutput(true);
                } else {
                    c.getSource().getEmbed()
                        .title("Failed")
                        .description("Another inventory action has taken priority this tick, try again")
                        .errorColor();
                }
                return OK;
            })))
            .then(literal("actionDelayTicks").then(argument("ticks", time(0, 100)).executes(c -> {
                CONFIG.client.inventory.actionDelayTicks = getInteger(c, "ticks");
                settingsEmbed(c.getSource().getEmbed()
                    .title("Action Delay Ticks Set"));
            })))
            .then(literal("ncpStrict").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.inventory.ncpStrict = getToggle(c, "toggle");
                settingsEmbed(c.getSource().getEmbed()
                    .title("NCP Strict " + toggleStrCaps(CONFIG.client.inventory.ncpStrict)));
            })))
            .then(literal("autoCloseOpenContainers")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.inventory.autoCloseOpenContainers = getToggle(c, "toggle");
                    settingsEmbed(c.getSource().getEmbed()
                        .title("Auto Close " + toggleStrCaps(CONFIG.client.inventory.autoCloseOpenContainers)));
                }))
                .then(literal("delaySeconds").then(argument("seconds", integer(1, 1000)).executes(c -> {
                    CONFIG.client.inventory.autoCloseOpenContainerAfterSeconds = getInteger(c, "seconds");
                    settingsEmbed(c.getSource().getEmbed()
                        .title("Auto Close Delay Set"));
                }))));
    }

    private int getOpenContainerId() {
        return CACHE.getPlayerCache().getInventoryCache().getOpenContainerId();
    }

    private int getHotbarSlot0() {
        if (getOpenContainerId() == 0) return 36;
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        return container.getSize() - 9;
    }

    private RequestFuture drop(final int slot, final boolean dropStack) {
        var actions = new ArrayList<InventoryAction>();
        if (CACHE.getPlayerCache().getInventoryCache().getMouseStack() != Container.EMPTY_STACK) {
            // drop the item in the mouse stack
            actions.add(new DropMouseStack(getOpenContainerId(), ClickItemAction.LEFT_CLICK));
        }
        actions.add(new DropItem(
            getOpenContainerId(),
            slot,
            dropStack ? DropItemAction.DROP_SELECTED_STACK : DropItemAction.DROP_FROM_SELECTED
        ));
        return INVENTORY.submit(InventoryActionRequest.builder()
            .owner(this)
            .actions(actions)
            .priority(INV_ACTION_PRIORITY)
            .build());
    }

    private void logInv() {
        final List<String> output = new ArrayList<>();
        printInvAscii(output, false);
        CommandOutputHelper.logMultiLineOutput(output);
    }

    private void printInvAscii(final List<String> multiLineOutput, final boolean showAllSlotIds) {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var isPlayerInv = container.getContainerId() == 0;
        int containerSize = container.getSize();
        String containerAscii;
        if (!isPlayerInv) {
            var containerTypeInfo = ContainerTypeInfoRegistry.REGISTRY.get(container.getType());
            if (containerTypeInfo == null) {
                multiLineOutput.add("Unknown container type: " + container.getType());
                return;
            }
            if (containerSize != containerTypeInfo.totalSlots()) {
                multiLineOutput.add("Container size mismatch: " + containerSize + " != " + containerTypeInfo.totalSlots());
                return;
            }
            containerAscii = containerTypeInfo.ascii();
        } else {
            if (containerSize != 46) {
                multiLineOutput.add("Player inventory size mismatch: " + containerSize + " != 46");
                return;
            }
            containerAscii = ContainerTypeInfoRegistry.playerInventoryAscii;
        }
        var sb = new StringBuilder();
        var slotsWithItems = new String[containerSize];
        Arrays.fill(slotsWithItems, "");
        sb.append("```\n");
        var heldSlot = CACHE.getPlayerCache().getHeldItemSlot() + 36;
        for (int i = 0; i < containerSize; i++) {
            var itemStack = container.getItemStack(i);
            if (itemStack == Container.EMPTY_STACK) continue;
            slotsWithItems[i] = i + "";
            var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
            sb.append("  ").append(i).append(" -> ");
            sb.append(itemData.name());
            var nameComponent = itemStack.getDataComponentsOrEmpty().get(DataComponentTypes.CUSTOM_NAME);
            if (nameComponent != null) {
                sb.append(" \"").append(ComponentSerializer.serializePlain(nameComponent)).append("\"");
            }
            if (itemStack.getAmount() > 1) sb.append(" (x").append(itemStack.getAmount()).append(")");
            if (i == heldSlot) sb.append(" [Held]");
            sb.append("\n");
        }
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (mouseStack != Container.EMPTY_STACK) {
            sb.append("  mouse -> ");
            var itemData = ItemRegistry.REGISTRY.get(mouseStack.getId());
            sb.append(itemData.name());
            var nameComponent = mouseStack.getDataComponentsOrEmpty().get(DataComponentTypes.CUSTOM_NAME);
            if (nameComponent != null) {
                sb.append(" \"").append(ComponentSerializer.serializePlain(nameComponent)).append("\"");
            }
            if (mouseStack.getAmount() > 1) sb.append(" (x").append(mouseStack.getAmount()).append(")");
            sb.append("\n");
        }
        sb.append("\n```");
        var items = sb.toString();
        if (showAllSlotIds) {
            String[] allSlots = new String[containerSize];
            for (int i = 0; i < allSlots.length; i++) {
                allSlots[i] = String.valueOf(i);
            }
            multiLineOutput.add(String.format(containerAscii, (Object[]) allSlots));
        } else {
            multiLineOutput.add(String.format(containerAscii, (Object[]) slotsWithItems));
        }
        if (items.isEmpty()) {
            multiLineOutput.add("Empty!");
        } else {
            if (items.length() > 1950) {
                List<String> lines = items.lines().toList();
                sb.setLength(0);
                sb.append("```\n");
                for (String line : lines) {
                    if (line.isBlank() || line.startsWith("```") || line.endsWith("```")) continue;
                    sb.append(line).append("\n");
                    if (sb.length() > 1500) {
                        sb.append("\n```");
                        multiLineOutput.add(sb.toString());
                        sb.setLength(0);
                        sb.append("```\n");
                    }
                }
                String s = sb.toString();
                if (!s.endsWith("```\n")) {
                    s += "\n```";
                    multiLineOutput.add(s);
                }
            } else {
                multiLineOutput.add(items);
            }
        }
    }

    private boolean verifyAbleToDoInvActions(final Embed embed) {
        if (Proxy.getInstance().isConnected() && !Proxy.getInstance().hasActivePlayer()) return true;
        embed
            .title("Error")
            .description("Unable to perform inventory actions while not logged in or while a player is controlling")
            .errorColor();
        return false;
    }

    private boolean verifyLoggedIn(final Embed embed) {
        var client = Proxy.getInstance().getClient();
        if (client == null || !Proxy.getInstance().isConnected()) {
            embed
                .title("Error")
                .description("Not logged in!")
                .errorColor();
            return false;
        }
        return true;
    }

    private void settingsEmbed(Embed embed) {
        embed
            .addField("Action Delay Ticks", CONFIG.client.inventory.actionDelayTicks)
            .addField("NCP Strict", CONFIG.client.inventory.ncpStrict)
            .addField("Auto Close Open Containers", CONFIG.client.inventory.autoCloseOpenContainers)
            .addField("Auto Close Open Containers Delay", CONFIG.client.inventory.autoCloseOpenContainerAfterSeconds + "s")
            .primaryColor();
    }
}
