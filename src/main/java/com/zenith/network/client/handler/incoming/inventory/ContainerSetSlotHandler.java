package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class ContainerSetSlotHandler implements ClientEventLoopPacketHandler<ClientboundContainerSetSlotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundContainerSetSlotPacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache().setInventorySlot(packet.getContainerId(), packet.getItem(), packet.getSlot());
        CACHE.getPlayerCache().getInventoryCache().getOpenContainer().setStateId(packet.getStateId());
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
