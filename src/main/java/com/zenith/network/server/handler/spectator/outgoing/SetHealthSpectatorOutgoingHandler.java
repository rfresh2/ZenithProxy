package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class SetHealthSpectatorOutgoingHandler implements PacketHandler<ClientboundSetHealthPacket, ServerConnection> {
    @Override
    public ClientboundSetHealthPacket apply(ClientboundSetHealthPacket packet, ServerConnection session) {
        return null;
    }
}
