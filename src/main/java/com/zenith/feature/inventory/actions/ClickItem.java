package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.ItemRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.*;

@Data
@RequiredArgsConstructor
public class ClickItem implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final ClickItemAction clickItemAction;
    private static final ContainerActionType containerActionType = ContainerActionType.CLICK_ITEM;

    public ClickItem(final int slotId, final ClickItemAction clickItemAction) {
        this(0, slotId, clickItemAction);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        ItemStack predictedMouseStack = Container.EMPTY_STACK;
        final ItemStack clickStack = container.getItemStack(slotId);
        if (isStackEmpty(mouseStack) && isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("Both mouse stack and click stack empty: {}", this);
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
                    if (clickStack == Container.EMPTY_STACK) {
                        // place one item from mouse stack into click stack
                        if (mouseStack.getAmount() == 1) {
                            predictedMouseStack = Container.EMPTY_STACK;
                            changedSlots.put(slotId, new ItemStack(mouseStack.getId(), mouseStack.getAmount(), mouseStack.getDataComponents()));
                        } else {
                            var newMouseStackAmount = mouseStack.getAmount() - 1;
                            predictedMouseStack = newMouseStackAmount == 0
                                ? Container.EMPTY_STACK
                                : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
                            changedSlots.put(slotId, new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents()));
                        }
                    } else {
                        // if both stacks are the same item, place one item from the mouse stack into clickStack
                        //   if clickStack is full, return null
                        if (mouseStack.getId() == clickStack.getId()) {
                            if (clickStack.getAmount() == ItemRegistry.REGISTRY.get(clickStack.getId()).stackSize()) return null;
                            var newMouseStackAmount = mouseStack.getAmount() - 1;
                            predictedMouseStack = newMouseStackAmount == 0
                                ? Container.EMPTY_STACK
                                : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
                            changedSlots.put(slotId, new ItemStack(clickStack.getId(), clickStack.getAmount() + 1, clickStack.getDataComponents()));
                        } else {
                            // if stacks are different, swap them
                            predictedMouseStack = clickStack;
                            changedSlots.put(slotId, mouseStack);
                        }
                    }
                }
            }
        }
        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? CACHE.getPlayerCache().getActionId().get() + 1
                : CACHE.getPlayerCache().getActionId().get(),
            slotId,
            containerActionType,
            clickItemAction,
            predictedMouseStack,
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
