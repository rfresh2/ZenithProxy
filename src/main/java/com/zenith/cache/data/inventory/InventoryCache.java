package com.zenith.cache.data.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import static com.zenith.Shared.CACHE_LOG;

@Data
public class InventoryCache {
    private final Int2ObjectMap<Container> containers = new Int2ObjectOpenHashMap<>();
    private int openContainerId = 0;
    @Nullable private ItemStack mouseStack = Container.EMPTY_STACK;

    public InventoryCache() {
        containers.put(0, new Container(0, 46));
    }

    public Container getPlayerInventory() {
        return containers.get(0);
    }

    public void setInventory(final int containerId, final ItemStack[] inventory) {
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
    }

    private void popContainer(final int containerId) { // assuming containerId > 0
        // populate player inventory based on prev container contents
        var container = containers.remove(containerId);
        if (container != containers.defaultReturnValue() && container.getSize() >= 36) {
            var playerInventory = getPlayerInventory();
            int playerIndex = 44;
            for (int containerIndex = container.getSize() - 1; containerIndex >= 0 && playerIndex >= 9; containerIndex--) {
                var stack = container.getItemStack(containerIndex);
                playerInventory.setItemStack(playerIndex, stack);
                playerIndex--;
            }
        }
        this.openContainerId = 0;
    }

    public void openContainer(final int containerId, final ContainerType type, final Component title) {
        if (containerId == 0) return;
        containers.put(containerId, new Container(containerId, type, title));
        this.openContainerId = containerId;
    }

    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        mouseStack = packet.getCarriedItem();
        var container = containers.get(packet.getContainerId());
        if (container == containers.defaultReturnValue()) {
            CACHE_LOG.debug("Attempted to click in unknown container {}", packet.getContainerId());
            return;
        }
        packet.getChangedSlots().forEach(container::setItemStack);
    }

    public void reset() {
        CACHE_LOG.debug("Resetting inventory cache");
        containers.clear();
        containers.put(0, new Container(0, 46));
        this.openContainerId = 0;
    }
}
