package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundPlaceGhostRecipePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class PlaceGhostRecipeSpectatorOutgoingHandler implements PacketHandler<ClientboundPlaceGhostRecipePacket, ServerConnection> {
    @Override
    public ClientboundPlaceGhostRecipePacket apply(ClientboundPlaceGhostRecipePacket packet, ServerConnection session) {
        return null;
    }
}
