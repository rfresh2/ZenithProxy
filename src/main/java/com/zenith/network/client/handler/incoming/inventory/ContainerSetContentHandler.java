package com.zenith.network.client.handler.incoming.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;


public class ContainerSetContentHandler implements AsyncPacketHandler<ClientboundContainerSetContentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundContainerSetContentPacket packet, @NonNull ClientSession session) {
        if (packet.getContainerId() == 0)  { // player inventory
            CACHE.getPlayerCache().setInventory(packet.getItems());
        }
        CACHE.getPlayerCache().getActionId().set(packet.getStateId());
        SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
