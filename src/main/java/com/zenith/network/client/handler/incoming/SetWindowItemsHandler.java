package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;


public class SetWindowItemsHandler implements AsyncIncomingHandler<ServerWindowItemsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerWindowItemsPacket packet, @NonNull ClientSession session) {
        if (packet.getWindowId() == 0)  { //player inventory
            CACHE.getPlayerCache().setInventory(packet.getItems());
        }
        SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }

    @Override
    public Class<ServerWindowItemsPacket> getPacketClass() {
        return ServerWindowItemsPacket.class;
    }
}
