package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ShiftClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

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

        // todo: find potential destination slot
        //  may cause anticheat issues as our changed slots are empty
        //  and may also cause cache issues as we are relying on the server to send us back updated slots

        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();

        if (action == ShiftClickItemAction.LEFT_CLICK) {
            if (container.getContainerId() == 0) {
                // if src is a hotbar slot, destination is the first empty slot in the main inventory
                //  and if src is an equippable item, destination is the first matching slot in armor equipment slots

                // if src is a main inventory slot, destination is the first empty hotbar slot
                //  and if src is an equippable item, destination is the first matching slot in armor equipment slots

                // if src is an equipment or crafting slot, destination is the first empty main inventory slot

                // and logic to combine stacked items
            } else {
                // very different logic depending on container type
            }
        }

        return new ServerboundContainerClickPacket(
            containerId,
            CACHE.getPlayerCache().getActionId().get() + 1, // todo: logic unhandled, always requesting full state response from server
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
