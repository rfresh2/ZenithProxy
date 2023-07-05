package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetSlotHandler implements HandlerRegistry.AsyncIncomingHandler<ServerSetSlotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSetSlotPacket packet, @NonNull ClientSession session) {
        if (packet.getWindowId() == 0 && packet.getSlot() >= 0) {
            CACHE.getPlayerCache().setInventorySlot(packet.getItem(), packet.getSlot());
            SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache();
        }
        return true;
    }
    @Override
    public Class<ServerSetSlotPacket> getPacketClass() {
        return ServerSetSlotPacket.class;
    }
}
