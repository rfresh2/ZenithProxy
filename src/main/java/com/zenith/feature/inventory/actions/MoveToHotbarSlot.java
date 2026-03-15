package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.*;

@Data
@RequiredArgsConstructor
public class MoveToHotbarSlot implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final MoveToHotbarAction moveToHotbarAction;
    private static final ContainerActionType containerActionType = ContainerActionType.MOVE_TO_HOTBAR_SLOT;

    public MoveToHotbarSlot(final int slotId, final MoveToHotbarAction moveToHotbarAction) {
        this(0, slotId, moveToHotbarAction);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (!isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("Can't move to hotbar, mouse stack is not empty: {}", this);
            return null; // can't swap if mouse stack is not empty
        }
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();
        int hotBarSlot = -1;
        boolean playerInv = containerId == 0;
        int hotbarOffset = playerInv ? 36 : container.getSize() - 9;
        switch (moveToHotbarAction) {
            case SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6, SLOT_7, SLOT_8, SLOT_9 -> {// swap the clickStack with the item in the hotbar slot
                hotBarSlot = moveToHotbarAction.getId() + hotbarOffset;
            }
            case OFF_HAND -> {
                if (playerInv) hotBarSlot = 45;
            }
            default -> {
                CLIENT_LOG.debug("Unhandled action param: {}", this);
                return null;
            }
        }
        final ItemStack clickStack = container.getItemStack(slotId);
        final ItemStack swapStack = hotBarSlot == -1
            ? CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND)
            : container.getItemStack(hotBarSlot);
        if (isStackEmpty(clickStack) && isStackEmpty(swapStack)) {
            CLIENT_LOG.debug("{} Can't swap two empty stacks with each other", this);
            return null; // can't swap if clickStack and swapStack is empty
        }
        if (hotBarSlot != -1) { // -1 == Swapping with offhand while in a container
            changedSlots.put(hotBarSlot, clickStack);
        }
        // there is no offhand slot id in the container, so only one slot is set as changed in the packet
        changedSlots.put(slotId, swapStack);

        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? CACHE.getPlayerCache().getActionId().get() + 1
                : CACHE.getPlayerCache().getActionId().get(),
            slotId,
            containerActionType,
            moveToHotbarAction,
            Container.EMPTY_STACK,
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
