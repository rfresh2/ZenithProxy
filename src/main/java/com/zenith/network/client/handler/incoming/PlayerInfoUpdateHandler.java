package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction.ADD_PLAYER;
import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class PlayerInfoUpdateHandler implements AsyncPacketHandler<ClientboundPlayerInfoUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerInfoUpdatePacket packet, @NonNull ClientSession session) {
        if (packet.getActions().contains(ADD_PLAYER)) {
            for (var entry : packet.getEntries()) {
                CACHE.getTabListCache().add(entry);
                EVENT_BUS.postAsync(new ServerPlayerConnectedEvent(entry));
            }
        }
        // todo: cache other actions state
        return true;
    }
}
