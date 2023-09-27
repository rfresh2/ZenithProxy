package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerPositionSpectatorHandler implements IncomingHandler<ServerboundMovePlayerPosPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundMovePlayerPosPacket packet, @NonNull ServerConnection session) {
        if (!session.isLoggedIn()) return false;
        session.getSpectatorPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorUtils.updateSpectatorPosition(session);
        SpectatorUtils.checkSpectatorPositionOutOfRender(session);
        return false;
    }
}
