package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DEFAULT_LOG;

public class PlayerChangeHeldItemHandler implements AsyncIncomingHandler<ServerPlayerChangeHeldItemPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerPlayerChangeHeldItemPacket packet, ClientSession session) {
        try {
            CACHE.getPlayerCache().setHeldItemSlot(packet.getSlot());
            SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
        } catch (final Exception e) {
            DEFAULT_LOG.error("failed updating main hand slot", e);
        }
        return true;
    }

    @Override
    public Class<ServerPlayerChangeHeldItemPacket> getPacketClass() {
        return ServerPlayerChangeHeldItemPacket.class;
    }
}
