package com.zenith.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class PlayerRotationSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientPlayerRotationPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ClientPlayerRotationPacket packet, @NonNull ServerConnection session) {
        session.getSpectatorPlayerCache()
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        PlayerPositionRotationSpectatorHandler.updateSpectatorPosition(session);
        return false;
    }

    @Override
    public Class<ClientPlayerRotationPacket> getPacketClass() {
        return ClientPlayerRotationPacket.class;
    }
}
