package com.zenith.cache.data.inventory;

import lombok.Data;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.CACHE_LOG;

@Data
public class Container {
    private int containerId;
    private List<ItemStack> contents;
    private ContainerType type = ContainerType.GENERIC_9X4;
    private Component title = Component.empty();
    public static final ItemStack EMPTY_STACK = null;

    public Container(int containerId, List<ItemStack> contents) {
        this.containerId = containerId;
        this.contents = contents;
    }

    public Container(int containerId, int size) {
        this.containerId = containerId;
        this.contents = initializeEmptyContents(size);
    }

    public Container(final int containerId, final ContainerType type, final Component title) {
        this.containerId = containerId;
        this.type = type;
        this.title = title;
        this.contents = initializeEmptyContents(0);
    }

    private List<ItemStack> initializeEmptyContents(int size) {
        final List<ItemStack> contents = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            contents.add(EMPTY_STACK);
        }
        return contents;
    }

    public void setContents(final ItemStack[] inventory) {
        if (inventory.length != contents.size()) {
            this.contents = initializeEmptyContents(inventory.length);
        }
        for (int i = 0; i < inventory.length; i++) {
            contents.set(i, inventory[i]);
        }
    }

    public void setItemStack(final int slot, final ItemStack newItemStack) {
        if (slot < 0 || slot >= contents.size()) {
            CACHE_LOG.debug("Invalid slot: {} for containerId: {} from size: {}", slot, containerId, contents.size());
            return;
        }
        contents.set(slot, newItemStack);
    }

    public ItemStack getItemStack(final int slot) {
        if (slot < 0 || slot >= contents.size()) {
            CACHE_LOG.debug("Invalid slot: {} for containerId: {} from size: {}", slot, containerId, contents.size());
            return EMPTY_STACK;
        }
        return contents.get(slot);
    }

    public int getSize() {
        return contents.size();
    }
}
