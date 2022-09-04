package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;


public class SetWindowItemsHandler implements HandlerRegistry.AsyncIncomingHandler<ServerWindowItemsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerWindowItemsPacket packet, @NonNull ClientSession session) {
        if (packet.getWindowId() == 0)  { //player inventory
            CACHE.getPlayerCache().setInventory(packet.getItems());
        }
        SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }

    @Override
    public Class<ServerWindowItemsPacket> getPacketClass() {
        return ServerWindowItemsPacket.class;
    }
}
