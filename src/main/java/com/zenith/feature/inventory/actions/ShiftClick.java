package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.inventory.menu.QuickMoveMenu;
import com.zenith.mc.item.hashing.ItemStackHasher;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ShiftClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.jspecify.annotations.Nullable;

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
                ItemStackHasher.hash(Container.EMPTY_STACK),
                new Int2ObjectArrayMap<@Nullable HashedStack>()
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
        var hashedChangedSlots = new Int2ObjectArrayMap<@Nullable HashedStack>();
        for (var e : changedSlots.int2ObjectEntrySet()) {
            hashedChangedSlots.put(e.getIntKey(), ItemStackHasher.hash(e.getValue()));
        }

        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? container.incrementStateId()
                : container.getStateId(),
            slotId,
            containerActionType,
            action,
            ItemStackHasher.hash(Container.EMPTY_STACK),
            hashedChangedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
