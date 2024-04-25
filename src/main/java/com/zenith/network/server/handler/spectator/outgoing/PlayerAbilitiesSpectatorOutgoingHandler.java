package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;

public class PlayerAbilitiesSpectatorOutgoingHandler implements PacketHandler<ClientboundPlayerAbilitiesPacket, ServerConnection> {
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
