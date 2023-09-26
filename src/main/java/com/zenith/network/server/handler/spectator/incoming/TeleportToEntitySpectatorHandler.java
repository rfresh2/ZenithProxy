package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class TeleportToEntitySpectatorHandler implements IncomingHandler<ServerboundTeleportToEntityPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundTeleportToEntityPacket packet, final ServerConnection session) {
        if (CACHE.getProfileCache().getProfile().getId().equals(packet.getTarget())) {
            SpectatorUtils.syncSpectatorPositionToPlayer(session);
        }
        return false;
    }
}
