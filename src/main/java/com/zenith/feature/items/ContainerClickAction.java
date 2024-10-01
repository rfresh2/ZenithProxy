package com.zenith.feature.items;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.ItemRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.*;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.jetbrains.annotations.Nullable;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public record ContainerClickAction(int slotId, ContainerActionType actionType, ContainerAction param) {
    public @Nullable ServerboundContainerClickPacket toPacket() {
        try {
            return switch (actionType()) {
                case CLICK_ITEM -> clickItem();
                case MOVE_TO_HOTBAR_SLOT -> moveToHotbarSlot();
                case DROP_ITEM -> dropItem();
                // todo: implement the other action types
                default -> {
                    CLIENT_LOG.debug("[{}, {}, {}] Unhandled container action type", slotId, actionType, param);
                    yield null;
                }
            };
        } catch (final Exception e) {
            CLIENT_LOG.error("Error processing container click action: {}", this, e);
            return null;
        }
    }

    private ServerboundContainerClickPacket clickItem() {
        if (slotId == -999) return clickDropItem(); // special case for dropping items
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        ItemStack predictedMouseStack = Container.EMPTY_STACK;
        if (!(param instanceof final ClickItemAction clickItemAction)) {
            CLIENT_LOG.debug("[{}, {}, {}] Invalid click item action", slotId, actionType, param);
            return null;
        }
        final ItemStack clickStack = CACHE.getPlayerCache().getPlayerInventory().get(slotId);
        if (isStackEmpty(mouseStack) && isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Both mouse stack and click stack empty", slotId, actionType, param);
            return null;
        }
        final Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();

        switch (clickItemAction) {
            case LEFT_CLICK -> {
                // swap the mouse stack with the item in slotId
                predictedMouseStack = clickStack;
                changedSlots.put(slotId, mouseStack);
            }
            case RIGHT_CLICK -> {
                // if mouse stack is empty, pick up half the clickStack
                if (isStackEmpty(mouseStack)) {
                    // round up to the nearest half stack
                    final int halfStackSize = (int) Math.ceil(clickStack.getAmount() / 2.0);
                    predictedMouseStack = new ItemStack(clickStack.getId(), halfStackSize, clickStack.getDataComponents());
                    changedSlots.put(slotId, new ItemStack(clickStack.getId(), clickStack.getAmount() - halfStackSize, clickStack.getDataComponents()));
                } else {
                    // if both stacks are the same item, place one item from the mouse stack into clickStack
                    //   if clickStack is full, return null
                    if (mouseStack.getId() == clickStack.getId()) {
                        if (clickStack.getAmount() == ItemRegistry.REGISTRY.get(clickStack.getId()).stackSize()) return null;
                        var newMouseStackAmount = mouseStack.getAmount() - 1;
                        predictedMouseStack = newMouseStackAmount == 0 ? Container.EMPTY_STACK : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
                        changedSlots.put(slotId, new ItemStack(clickStack.getId(), clickStack.getAmount() + 1, clickStack.getDataComponents()));
                    } else {
                        // if stacks are different, swap them
                        predictedMouseStack = clickStack;
                        changedSlots.put(slotId, mouseStack);
                    }
                }
            }
        }
        return new ServerboundContainerClickPacket(
            0,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            slotId,
            actionType,
            param,
            predictedMouseStack,
            changedSlots
        );
    }
    /**
     * Must have item in mouse stack
     *  Drop 1: right click
     * [2024/03/12 01:53:29] [Server] [DEBUG] [1710233609699] [ServerConnection] Received: ServerboundContainerClickPacket(containerId=0, stateId=4, slot=-999, action=CLICK_ITEM, param=RIGHT_CLICK, carriedItem=ItemStack(id=14, amount=35, nbt=null), changedSlots={})
     *
     * Drop stack: left click
     * [2024/03/12 01:53:32] [Server] [DEBUG] [1710233612797] [ServerConnection] Received: ServerboundContainerClickPacket(containerId=0, stateId=4, slot=-999, action=CLICK_ITEM, param=LEFT_CLICK, carriedItem=null, changedSlots={})
     */
    private ServerboundContainerClickPacket clickDropItem() {
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't drop empty mouse stack", slotId, actionType, param);
            return null; // can't drop if mouse stack is empty
        }
        ItemStack predictedMouseStack = Container.EMPTY_STACK;
        if (!(param instanceof final ClickItemAction clickItemAction)) {
            CLIENT_LOG.debug("[{}, {}, {}] Not ClickItemAction param", slotId, actionType, param);
            return null;
        }
        predictedMouseStack = switch (clickItemAction) {
            case LEFT_CLICK -> // drop the entire stack from the mouse stack
                Container.EMPTY_STACK;
            case RIGHT_CLICK -> // drop 1 item from the mouse stack
                mouseStack.getAmount() == 1
                    ? Container.EMPTY_STACK
                    : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
        };
        return new ServerboundContainerClickPacket(
            0,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            -999,
            actionType,
            param,
            predictedMouseStack,
            Int2ObjectMaps.emptyMap()
        );
    }

    private ServerboundContainerClickPacket moveToHotbarSlot() {
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (!isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't move to hotbar, mouse stack is not empty", slotId, actionType, param);
            return null; // can't swap if mouse stack is not empty
        }
        if (!(param instanceof MoveToHotbarAction moveToHotbarAction)) {
            CLIENT_LOG.debug("[{}, {}, {}] Not MoveToHotbarAction", slotId, actionType, param);
            return null;
        }
        final ItemStack clickStack = CACHE.getPlayerCache().getPlayerInventory().get(slotId);
        if (isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't swap empty stack", slotId, actionType, param);
            return null; // can't swap if clickStack is empty
        }
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();
        int hotBarSlot = -1;
        switch (moveToHotbarAction) {
            case SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6, SLOT_7, SLOT_8, SLOT_9 -> // swap the clickStack with the item in the hotbar slot
                hotBarSlot = moveToHotbarAction.getId() + 36;
            case OFF_HAND -> hotBarSlot = 45;
            default -> {
                CLIENT_LOG.debug("[{}, {}, {}] Unhandled action param", slotId, actionType, param);
                return null;
            }
        }
        final ItemStack swapStack = CACHE.getPlayerCache().getPlayerInventory().get(hotBarSlot);
        changedSlots.put(hotBarSlot, clickStack);
        if (!isStackEmpty(swapStack)) {
            changedSlots.put(slotId, swapStack);
        }

        return new ServerboundContainerClickPacket(
            0,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            slotId,
            actionType,
            param,
            Container.EMPTY_STACK,
            changedSlots
        );
    }

    private ServerboundContainerClickPacket dropItem() {
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (!isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't drop as mouse stack not empty", slotId, actionType, param);
            return null; // can't drop if mouse stack is not empty
        }
        if (!(param instanceof DropItemAction dropItemAction)) {
            CLIENT_LOG.debug("[{}, {}, {}] Not DropItemAction", slotId, actionType, param);
            return null;
        }
        final ItemStack clickStack = CACHE.getPlayerCache().getPlayerInventory().get(slotId);
        if (isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't drop empty click stack", slotId, actionType, param);
            return null; // can't drop if clickStack is empty
        }
        final Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();

        switch (dropItemAction) {
            case DROP_FROM_SELECTED -> // drop 1 item from the selected slot
                changedSlots.put(
                    slotId,
                    clickStack.getAmount() == 1
                        ? Container.EMPTY_STACK
                        : new ItemStack(clickStack.getId(), clickStack.getAmount() - 1, clickStack.getDataComponents()));
            case DROP_SELECTED_STACK -> // drop the entire stack from the selected slot
                changedSlots.put(slotId, Container.EMPTY_STACK);
            default -> { return null; }
        }
        return new ServerboundContainerClickPacket(
            0,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            slotId,
            actionType,
            param,
            Container.EMPTY_STACK,
            changedSlots
        );
    }

    private boolean isStackEmpty(ItemStack stack) {
        return stack == Container.EMPTY_STACK;
    }
}
