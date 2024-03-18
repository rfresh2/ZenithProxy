package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerPositionRotationSpectatorHandler implements PacketHandler<ServerboundMovePlayerPosRotPacket, ServerConnection> {
    @Override
    public ServerboundMovePlayerPosRotPacket apply(@NonNull ServerboundMovePlayerPosRotPacket packet, @NonNull ServerConnection session) {
        if (!session.isLoggedIn()) return null;
        session.getSpectatorPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorSync.updateSpectatorPosition(session);
        SpectatorSync.checkSpectatorPositionOutOfRender(session);
        return null;
    }
}
