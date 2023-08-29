package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.List;

import static com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction.ADD_PLAYER;
import static com.zenith.Shared.CACHE;

public class PlayerInfoUpdateHandler implements AsyncIncomingHandler<ClientboundPlayerInfoUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerInfoUpdatePacket packet, @NonNull ClientSession session) {
        if (packet.getActions().contains(ADD_PLAYER)) {
            List.of(packet.getEntries()).forEach(CACHE.getTabListCache().getTabList()::add);
        }
        return true;
    }

    @Override
    public Class<ClientboundPlayerInfoUpdatePacket> getPacketClass() {
        return ClientboundPlayerInfoUpdatePacket.class;
    }
}
