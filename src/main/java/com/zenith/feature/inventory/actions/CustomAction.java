package com.zenith.feature.inventory.actions;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.jspecify.annotations.Nullable;

/**
 * Send a custom-supplied packet, perhaps for an action not covered by the existing types.
 * or if you want to run arbitrary code at execution time instead of a normal action
 */
@Data
@RequiredArgsConstructor
public class CustomAction implements InventoryAction {
    private final int containerId;
    private final PacketSupplier packetSupplier;

    @Override
    public int containerId() {
        return containerId;
    }

    @Override
    public @Nullable MinecraftPacket packet() {
        return packetSupplier.get();
    }

    @FunctionalInterface
    public interface PacketSupplier {
        @Nullable MinecraftPacket get();
    }
}
