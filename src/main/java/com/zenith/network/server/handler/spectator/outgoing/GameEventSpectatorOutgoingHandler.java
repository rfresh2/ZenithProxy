package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class GameEventSpectatorOutgoingHandler implements OutgoingHandler<ClientboundGameEventPacket, ServerConnection> {
    @Override
    public ClientboundGameEventPacket apply(final ClientboundGameEventPacket packet, final ServerConnection session) {
        if (packet.getNotification() == GameEvent.CHANGE_GAMEMODE) return null;
        return packet;
    }

    @Override
    public Class<ClientboundGameEventPacket> getPacketClass() {
        return ClientboundGameEventPacket.class;
    }
}
