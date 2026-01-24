package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.*;

@Data
@RequiredArgsConstructor
public class DropItem implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final DropItemAction dropItemAction;
    private static final ContainerActionType containerActionType = ContainerActionType.DROP_ITEM;

    public DropItem(final int slotId, final DropItemAction dropItemAction) {
        this(0, slotId, dropItemAction);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (!isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("Can't drop because mouse stack is not empty: {}", this);
            return null; // can't drop if mouse stack is not empty
        }
        final ItemStack clickStack = container.getItemStack(slotId);
        if (isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("Can't drop empty click stack: {}", this);
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
            default -> {
                CLIENT_LOG.debug("Unhandled drop item action: {}", this);
                return null;
            }
        }
        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? CACHE.getPlayerCache().getActionId().get() + 1
                : CACHE.getPlayerCache().getActionId().get(),
            slotId,
            containerActionType,
            dropItemAction,
            Container.EMPTY_STACK,
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
