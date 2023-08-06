package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DEFAULT_LOG;

public class PostOutgoingPlayerChangeHeldItemHandler implements PostOutgoingHandler<ClientPlayerChangeHeldItemPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerChangeHeldItemPacket packet, ClientSession session) {
        try {
            CACHE.getPlayerCache().setHeldItemSlot(packet.getSlot());
            SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
        } catch (final Exception e) {
            DEFAULT_LOG.error("failed updating main hand slot", e);
        }
    }

    @Override
    public Class<ClientPlayerChangeHeldItemPacket> getPacketClass() {
        return ClientPlayerChangeHeldItemPacket.class;
    }
}
