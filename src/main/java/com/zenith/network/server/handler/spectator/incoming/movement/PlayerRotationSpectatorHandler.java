package com.zenith.network.server.handler.spectator.incoming.movement;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

public class PlayerRotationSpectatorHandler implements PacketHandler<ServerboundMovePlayerRotPacket, ServerSession> {
    @Override
    public ServerboundMovePlayerRotPacket apply(@NonNull ServerboundMovePlayerRotPacket packet, @NonNull ServerSession session) {
        if (!session.isLoggedIn()) return null;
        session.getSpectatorPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorSync.updateSpectatorPosition(session);
        return null;
    }
}
