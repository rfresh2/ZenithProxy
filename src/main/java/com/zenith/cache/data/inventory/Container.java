package com.zenith.cache.data.inventory;

import com.zenith.mc.item.hashing.ItemStackHasher;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zenith.Globals.CACHE_LOG;

@Data
public class Container {
    private int containerId;
    private List<ItemStack> contents;
    private ContainerType type = ContainerType.GENERIC_9X4;
    private Component title = Component.empty();
    public static final ItemStack EMPTY_STACK = null;
    private final AtomicInteger stateId = new AtomicInteger(0);

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

    // returns a cloned itemstack
    // so we can still retain itemstack field mutability
    public ItemStack hashToMatchingStack(HashedStack hashedStack) {
        for (int i = 0; i < contents.size(); i++) {
            ItemStack itemStack = contents.get(i);
            if (ItemStackHasher.matches(hashedStack, itemStack)) {
                if (itemStack != null) {
                    // defensively clone the itemStack
                    return new ItemStack(
                        itemStack.getId(),
                        itemStack.getAmount(),
                        itemStack.getDataComponents() != null
                            ? itemStack.getDataComponents().clone()
                            : null);
                }
                return null;
            }
        }
        return null;
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

    public int getStateId() {
        return this.stateId.get();
    }

    @Deprecated
    public AtomicInteger getStateIdAtomic() {
        return this.stateId;
    }

    public int incrementStateId() {
        return this.stateId.updateAndGet(v -> v + 1 & 32767);
    }

    public void setStateId(final int stateId) {
        this.stateId.set(stateId);
    }
}
