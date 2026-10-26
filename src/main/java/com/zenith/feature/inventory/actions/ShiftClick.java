package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.inventory.menu.QuickMoveMenu;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ShiftClickItemAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.*;

@Data
@RequiredArgsConstructor
public class ShiftClick implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final ShiftClickItemAction action;
    private static final ContainerActionType containerActionType = ContainerActionType.SHIFT_CLICK_ITEM;

    public ShiftClick(final int slotId, final ShiftClickItemAction action) {
        this(0, slotId, action);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();

        if (!isStackEmpty(CACHE.getPlayerCache().getInventoryCache().getMouseStack())) {
            CLIENT_LOG.debug("Can't shift click, mouse stack is not empty: {}", this);
            return null;
        }
        var itemStack = container.getItemStack(slotId);
        if (isStackEmpty(itemStack)) {
            CLIENT_LOG.debug("Can't shift click, item stack is empty: {}", this);
            return null;
        }

        var menu = QuickMoveMenu.forContainer(container);
        if (menu == null || !menu.canSimulate(slotId)) {
            // not simulated, request full state from server
            return new ServerboundContainerClickPacket(
                containerId,
                container.incrementStateId(),
                slotId,
                containerActionType,
                action,
                Container.EMPTY_STACK,
                new Int2ObjectArrayMap<>()
            );
        }

        if (!menu.mayPickup(slotId)) {
            CLIENT_LOG.debug("Can't shift click, item can't be picked up: {}", this);
            return null;
        }

        var changedSlots = menu.quickMove(slotId);
        if (changedSlots.isEmpty()) {
            CLIENT_LOG.debug("Can't shift click, no destination slot available: {}", this);
            return null;
        }

        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? container.incrementStateId()
                : container.getStateId(),
            slotId,
            containerActionType,
            action,
            Container.EMPTY_STACK,
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
