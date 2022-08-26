package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class PlayerStateSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientPlayerStatePacket, ServerConnection> {

    @Override
    public boolean apply(ClientPlayerStatePacket packet, ServerConnection session) {
        if (packet.getState() == PlayerState.START_SNEAKING || packet.getState() == PlayerState.START_SPRINTING) {
            session.getSoundPacket().ifPresent(p -> {
                session.getProxy().getServerConnections().forEach(connection -> connection.send(p));
            });
        }
        return false;
    }

    @Override
    public Class<ClientPlayerStatePacket> getPacketClass() {
        return ClientPlayerStatePacket.class;
    }
}
