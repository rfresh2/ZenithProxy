package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundPlaceGhostRecipePacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class PlaceGhostRecipeSpectatorOutgoingHandler implements OutgoingHandler<ClientboundPlaceGhostRecipePacket, ServerConnection> {
    @Override
    public ClientboundPlaceGhostRecipePacket apply(ClientboundPlaceGhostRecipePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundPlaceGhostRecipePacket> getPacketClass() {
        return ClientboundPlaceGhostRecipePacket.class;
    }
}
