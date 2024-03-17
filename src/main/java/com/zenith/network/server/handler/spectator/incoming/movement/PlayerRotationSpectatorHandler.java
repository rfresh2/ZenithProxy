package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerRotationSpectatorHandler implements PacketHandler<ServerboundMovePlayerRotPacket, ServerConnection> {
    @Override
    public ServerboundMovePlayerRotPacket apply(@NonNull ServerboundMovePlayerRotPacket packet, @NonNull ServerConnection session) {
        if (!session.isLoggedIn()) return null;
        session.getSpectatorPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorSync.updateSpectatorPosition(session);
        return null;
    }
}
