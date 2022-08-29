package com.zenith.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.DEFAULT_LOG;

public class HeldItemChangePostHandler implements HandlerRegistry.AsyncIncomingHandler<ClientPlayerChangeHeldItemPacket, ServerConnection> {

    @Override
    public boolean applyAsync(ClientPlayerChangeHeldItemPacket packet, ServerConnection session) {
        try {
            CACHE.getPlayerCache().getThePlayer().getEquipment().put(EquipmentSlot.MAIN_HAND, CACHE.getPlayerCache().getInventory()[packet.getSlot() + 36]);
            SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache(session.getProxy());
        } catch (final Exception e) {
            DEFAULT_LOG.error("Failed updating held item", e);
        }
        return true;
    }

    @Override
    public Class<ClientPlayerChangeHeldItemPacket> getPacketClass() {
        return ClientPlayerChangeHeldItemPacket.class;
    }
}
