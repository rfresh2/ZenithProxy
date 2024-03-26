package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DEFAULT_LOG;

public class PostOutgoingSetCarriedItemHandler implements ClientEventLoopPacketHandler<ServerboundSetCarriedItemPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerboundSetCarriedItemPacket packet, ClientSession session) {
        try {
            CACHE.getPlayerCache().setHeldItemSlot(packet.getSlot());
            SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        } catch (final Exception e) {
            DEFAULT_LOG.error("failed updating main hand slot", e);
        }
        return true;
    }
}
