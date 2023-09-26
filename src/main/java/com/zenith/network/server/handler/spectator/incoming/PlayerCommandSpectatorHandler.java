package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

public class PlayerCommandSpectatorHandler implements IncomingHandler<ServerboundPlayerCommandPacket, ServerConnection> {

    @Override
    public boolean apply(ServerboundPlayerCommandPacket packet, ServerConnection session) {
        if (session.isPlayerCam()) {
            if (packet.getState() == PlayerState.START_SNEAKING) {
                session.setPlayerCam(false);
                session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
                SpectatorUtils.syncSpectatorPositionToPlayer(session);
            }
        } else {
            if (packet.getState() == PlayerState.START_SNEAKING || packet.getState() == PlayerState.START_SPRINTING) {
                session.getSoundPacket().ifPresent(p -> {
                    session.getProxy().getActiveConnections().forEach(connection -> connection.send(p));
                });
            }
        }
        return false;
    }
}
