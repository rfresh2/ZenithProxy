package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerRotationSpectatorHandler implements IncomingHandler<ServerboundMovePlayerRotPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundMovePlayerRotPacket packet, @NonNull ServerConnection session) {
        if (session.isLoggedIn()) return false;
        session.getSpectatorPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        PlayerPositionRotationSpectatorHandler.updateSpectatorPosition(session);
        return false;
    }
}
