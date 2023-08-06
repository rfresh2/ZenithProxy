package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetSlotHandler implements AsyncIncomingHandler<ServerSetSlotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSetSlotPacket packet, @NonNull ClientSession session) {
        if (packet.getWindowId() == 0 && packet.getSlot() >= 0) {
            CACHE.getPlayerCache().setInventorySlot(packet.getItem(), packet.getSlot());
            SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
        }
        return true;
    }
    @Override
    public Class<ServerSetSlotPacket> getPacketClass() {
        return ServerSetSlotPacket.class;
    }
}
