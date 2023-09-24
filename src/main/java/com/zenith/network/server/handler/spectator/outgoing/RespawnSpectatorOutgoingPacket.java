package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class RespawnSpectatorOutgoingPacket implements OutgoingHandler<ClientboundRespawnPacket, ServerConnection> {
    @Override
    public ClientboundRespawnPacket apply(final ClientboundRespawnPacket packet, final ServerConnection session) {
        return new ClientboundRespawnPacket(
            packet.getDimension(),
            packet.getWorldName(),
            packet.getHashedSeed(),
            GameMode.SPECTATOR,
            GameMode.SPECTATOR,
            packet.isDebug(),
            packet.isFlat(),
            packet.isKeepMetadata(),
            packet.isKeepAttributes(),
            packet.getLastDeathPos(),
            packet.getPortalCooldown()
            );
    }
}
