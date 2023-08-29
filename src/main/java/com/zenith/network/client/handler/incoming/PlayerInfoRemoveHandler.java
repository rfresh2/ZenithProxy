package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class PlayerInfoRemoveHandler implements AsyncIncomingHandler<ClientboundPlayerInfoRemovePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundPlayerInfoRemovePacket packet, ClientSession session) {
        packet.getProfileIds().forEach(CACHE.getTabListCache().getTabList()::remove);
        return true;
    }

    @Override
    public Class<ClientboundPlayerInfoRemovePacket> getPacketClass() {
        return ClientboundPlayerInfoRemovePacket.class;
    }
}
