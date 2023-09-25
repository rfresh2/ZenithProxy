package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerCommandHandler implements PostOutgoingHandler<ServerboundPlayerCommandPacket, ClientSession> {
    @Override
    public void accept(final ServerboundPlayerCommandPacket packet, final ClientSession session) {
        if (packet.getEntityId() != CACHE.getPlayerCache().getEntityId()) return;
        switch (packet.getState()) {
            case START_SNEAKING -> CACHE.getPlayerCache().setSneaking(true);
            case STOP_SNEAKING -> CACHE.getPlayerCache().setSneaking(false);
            case START_SPRINTING -> CACHE.getPlayerCache().setSprinting(true);
            case STOP_SPRINTING -> CACHE.getPlayerCache().setSprinting(false);
        }
    }
}
