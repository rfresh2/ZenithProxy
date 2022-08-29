package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.DEFAULT_LOG;

public class PlayerChangeHeldItemHandler implements HandlerRegistry.AsyncIncomingHandler<ServerPlayerChangeHeldItemPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerPlayerChangeHeldItemPacket packet, ClientSession session) {
        try {
            CACHE.getPlayerCache().getThePlayer().getEquipment().put(EquipmentSlot.MAIN_HAND, CACHE.getPlayerCache().getInventory()[packet.getSlot() + 36]);
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
