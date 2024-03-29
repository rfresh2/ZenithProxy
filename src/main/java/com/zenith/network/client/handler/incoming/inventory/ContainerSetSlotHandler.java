package com.zenith.network.client.handler.incoming.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class ContainerSetSlotHandler implements ClientEventLoopPacketHandler<ClientboundContainerSetSlotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundContainerSetSlotPacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache().setInventorySlot(packet.getContainerId(), packet.getItem(), packet.getSlot());
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        CACHE.getPlayerCache().getActionId().set(packet.getStateId());
        return true;
    }
}
