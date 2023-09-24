package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class PlayerAbilitiesSpectatorOutgoingHandler implements OutgoingHandler<ClientboundPlayerAbilitiesPacket, ServerConnection> {
    @Override
    public ClientboundPlayerAbilitiesPacket apply(final ClientboundPlayerAbilitiesPacket packet, final ServerConnection session) {
        if (session.isLoggedIn()) return new ClientboundPlayerAbilitiesPacket(
            session.getSpectatorPlayerCache().isInvincible(),
            session.getSpectatorPlayerCache().isCanFly(),
            session.getSpectatorPlayerCache().isFlying(),
            session.getSpectatorPlayerCache().isCreative(),
            session.getSpectatorPlayerCache().getFlySpeed(),
            session.getSpectatorPlayerCache().getWalkSpeed()
        );
        return packet;
    }
}
