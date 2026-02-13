package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.BundleContents;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ItemTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
                if (!isStackEmpty(mouseStack) && ItemRegistry.REGISTRY.get(mouseStack.getId()).itemTags().contains(ItemTags.BUNDLES)) {
                    var mouseStackComponents = mouseStack.getDataComponentsOrEmpty();
                    var items = new ArrayList<>(mouseStackComponents.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, Collections.emptyList()));
                    var bundleContents = new BundleContents(items);
                    var clickStackCopy = clickStack.clone();
                    var result = bundleContents.tryInsert(clickStackCopy);
                    if (result != 0) {
                        var mouseStackCopy = mouseStack.clone();
                        if (mouseStackCopy.getDataComponents() == null) {
                            mouseStackCopy = new ItemStack(mouseStack.getId(), mouseStack.getAmount(), new DataComponents(new HashMap<>()));
                        }
                        mouseStackCopy.getDataComponents().put(DataComponentTypes.BUNDLE_CONTENTS, items);
                        predictedMouseStack = mouseStackCopy;
                        changedSlots.put(slotId, clickStackCopy.getAmount() == 0 ? Container.EMPTY_STACK : clickStackCopy);
                    }
                } else {
                    // swap the mouse stack with the item in slotId
                    predictedMouseStack = clickStack;
                    changedSlots.put(slotId, mouseStack);
                }
            }
            case RIGHT_CLICK -> {
                // if mouse stack is empty, pick up half the clickStack
                if (isStackEmpty(mouseStack)) {
                    // round up to the nearest half stack
                    final int halfStackSize = (int) Math.ceil(clickStack.getAmount() / 2.0);
                    predictedMouseStack = new ItemStack(clickStack.getId(), halfStackSize, clickStack.getDataComponents());
                    changedSlots.put(slotId, new ItemStack(clickStack.getId(), clickStack.getAmount() - halfStackSize, clickStack.getDataComponents()));
                } else {
                    if (!isStackEmpty(mouseStack) && ItemRegistry.REGISTRY.get(mouseStack.getId()).itemTags().contains(ItemTags.BUNDLES)) {
                        if (isStackEmpty(clickStack)) {
                            var mouseStackComponents = mouseStack.getDataComponentsOrEmpty();
                            var items = new ArrayList<>(mouseStackComponents.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, Collections.emptyList()));
                            var bundleContents = new BundleContents(items);
                            var itemToPlace = bundleContents.removeOne();
                            var newMouseComponents = mouseStackComponents.clone();
                            newMouseComponents.put(DataComponentTypes.BUNDLE_CONTENTS, items);
                            predictedMouseStack = new ItemStack(mouseStack.getId(), mouseStack.getAmount(), newMouseComponents);
                            changedSlots.put(slotId, itemToPlace);
                        } else {
                            // swap the mouse stack with the item in slotId
                            predictedMouseStack = clickStack;
                            changedSlots.put(slotId, mouseStack);
                        }
                    } else if (isStackEmpty(clickStack)) {
                        // place one item from mouse stack into click stack
                        if (!isStackEmpty(mouseStack) && mouseStack.getAmount() == 1) {
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
                        if (!isStackEmpty(mouseStack) && mouseStack.getId() == clickStack.getId()) {
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
