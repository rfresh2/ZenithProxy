package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
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
public class DropMouseStack implements InventoryAction {
    private final int containerId;
    private final ClickItemAction clickItemAction;
    private static final ContainerActionType containerActionType = ContainerActionType.CLICK_ITEM;

    public DropMouseStack(final ClickItemAction clickItemAction) {
        this(0, clickItemAction);
    }

    @Override
    public MinecraftPacket packet() {
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("Can't drop empty mouse stack: {}", this);
            return null; // can't drop if mouse stack is empty
        }
        ItemStack predictedMouseStack = Container.EMPTY_STACK;
        predictedMouseStack = switch (clickItemAction) {
            case LEFT_CLICK -> // drop the entire stack from the mouse stack
                Container.EMPTY_STACK;
            case RIGHT_CLICK -> // drop 1 item from the mouse stack
                mouseStack.getAmount() == 1
                    ? Container.EMPTY_STACK
                    : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
        };
        return new ServerboundContainerClickPacket(
            containerId,
            CONFIG.debug.inventoryRequestServerSyncOnAction
                ? CACHE.getPlayerCache().getActionId().get() + 1
                : CACHE.getPlayerCache().getActionId().get(),
            -999,
            containerActionType,
            clickItemAction,
            predictedMouseStack,
            Int2ObjectMaps.emptyMap()
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
