package com.zenith.network.client.handler.incoming.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache;


public class ContainerSetContentHandler implements AsyncPacketHandler<ClientboundContainerSetContentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundContainerSetContentPacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache().setInventory(packet.getContainerId(), packet.getItems());
        CACHE.getPlayerCache().getActionId().set(packet.getStateId());
        syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
