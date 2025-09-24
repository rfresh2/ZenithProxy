package com.zenith.feature.inventory.actions;

import lombok.Data;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;
import org.jspecify.annotations.Nullable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

@Data
public class RenameItem implements InventoryAction {
    private final int containerId;
    private final String name;

    @Override
    public @Nullable MinecraftPacket packet() {
        var currentContainerType = CACHE.getPlayerCache().getInventoryCache().getOpenContainer().getType();
        if (currentContainerType != ContainerType.ANVIL) {
            CLIENT_LOG.debug("Can only rename item while an anvil is open, current container type: {}", currentContainerType);
            return null; // can only rename in anvil
        }
        return new ServerboundRenameItemPacket(name);
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
