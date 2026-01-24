package com.zenith.cache.data.inventory;

import com.zenith.Proxy;
import com.zenith.mc.item.ContainerTypeInfoRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.jspecify.annotations.Nullable;

import static com.zenith.Globals.CACHE_LOG;

@Data
@Accessors(chain = true)
public class InventoryCache {
    private final Int2ObjectMap<Container> containers = new Int2ObjectOpenHashMap<>();
    private int openContainerId = 0;
    private int activeContainerId = -1;
    private long containerOpenedAt = 0L;
    private long lastContainerClick = 0L;
    @Nullable private ItemStack mouseStack = Container.EMPTY_STACK;

    public InventoryCache() {
        containers.put(0, new Container(0, 46));
    }

    public Container getPlayerInventory() {
        return containers.get(0);
    }

    public Container getOpenContainer() {
        var container = containers.get(openContainerId);
        if (container == null) {
            CACHE_LOG.warn("Attempted to get open container {} but it was null", openContainerId);
            return getPlayerInventory();
        } else {
            return container;
        }
    }

    private void setOpenContainerId(int containerId) {
        if (containerId == 0) {
            this.openContainerId = 0;
            this.containerOpenedAt = 0L;
        } else {
            this.openContainerId = containerId;
            this.containerOpenedAt = System.currentTimeMillis();
        }
    }

    public void setInventory(final int containerId, final ItemStack[] inventory) {
        if (containerId == 0 && inventory.length != 46)
            CACHE_LOG.debug("Setting player inventory with unexpected size: {}", inventory.length);
        containers.compute(containerId, (id, container) -> {
            if (container == null) {
                container = new Container(containerId, inventory.length);
            }
            container.setContents(inventory);
            return container;
        });
    }

    public void setItemStack(final int containerId, final int slot, final ItemStack newItemStack) {
        if (containerId == -1) {
            setMouseStack(newItemStack);
        } else if (containerId == -2) {
            getPlayerInventory().setItemStack(slot, newItemStack);
        } else {
            var container = containers.get(containerId);
            if (container != containers.defaultReturnValue()) {
                container.setItemStack(slot, newItemStack);
            } else {
                CACHE_LOG.debug("Attempted to set itemstack for unknown container {}", containerId);
            }
        }
    }

    public void closeContainer(final int containerId) {
        if (containerId == 0) return;
        popContainer(containerId);
        activeContainerId = -1;
    }

    private void popContainer(final int containerId) { // assuming containerId > 0
        // populate player inventory based on prev container contents
        var container = containers.remove(containerId);
        // todo: do containers act like a stack? or always pop to player inventory?
        if (container != containers.defaultReturnValue() && container.getSize() >= 36) {
            var playerInventory = getPlayerInventory();
            int playerIndex = 44;
            var containerTypeInfo = ContainerTypeInfoRegistry.REGISTRY.get(container.getType());
            for (int containerIndex = container.getSize() - containerTypeInfo.bottomSlots() - 1; containerIndex >= 0 && playerIndex >= 9; containerIndex--) {
                var stack = container.getItemStack(containerIndex);
                playerInventory.setItemStack(playerIndex, stack);
                playerIndex--;
            }
        }
        setOpenContainerId(0);
        if (!Proxy.getInstance().hasActivePlayer()) CACHE_LOG.info("Container {} closed", containerId);
    }

    public void openContainer(final int containerId, final ContainerType type, final Component title) {
        if (containerId == 0) return;
        containers.put(containerId, new Container(containerId, type, title));
        setOpenContainerId(containerId);
        activeContainerId = containerId;
        if (!Proxy.getInstance().hasActivePlayer()) CACHE_LOG.info("Container {} opened: {}", containerId, type);
    }

    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        mouseStack = packet.getCarriedItem();
        var container = containers.get(packet.getContainerId());
        if (container == containers.defaultReturnValue()) {
            CACHE_LOG.debug("Attempted to click in unknown container {}", packet.getContainerId());
            return;
        }
        if (packet.getContainerId() != 0) {
            if (packet.getActionType() == ContainerActionType.MOVE_TO_HOTBAR_SLOT
                && packet.getActionParam() instanceof MoveToHotbarAction hotbarAction
                && hotbarAction == MoveToHotbarAction.OFF_HAND
            ) {
                // offhand slot change is not sent in changed slots map as its not a slot in the container
                getPlayerInventory().setItemStack(45, container.getItemStack(packet.getSlot()));
            }
        }
        packet.getChangedSlots().forEach(container::setItemStack);
        lastContainerClick = System.currentTimeMillis();
        activeContainerId = packet.getContainerId();
    }

    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
        Container playerInventory = getPlayerInventory();
        playerInventory.setItemStack(packet.getSlot(), packet.getClickedItem());
    }

    public synchronized void reset() {
        CACHE_LOG.debug("Resetting inventory cache");
        containers.clear();
        containers.put(0, new Container(0, 46));
        setOpenContainerId(0);
        activeContainerId = -1;
    }
}
