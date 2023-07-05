package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DEFAULT_LOG;

public class PostOutgoingPlayerChangeHeldItemHandler implements HandlerRegistry.PostOutgoingHandler<ClientPlayerChangeHeldItemPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerChangeHeldItemPacket packet, ClientSession session) {
        try {
            CACHE.getPlayerCache().setHeldItemSlot(packet.getSlot());
            SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache();
        } catch (final Exception e) {
            DEFAULT_LOG.error("failed updating main hand slot", e);
        }
    }

    @Override
    public Class<ClientPlayerChangeHeldItemPacket> getPacketClass() {
        return ClientPlayerChangeHeldItemPacket.class;
    }
}
