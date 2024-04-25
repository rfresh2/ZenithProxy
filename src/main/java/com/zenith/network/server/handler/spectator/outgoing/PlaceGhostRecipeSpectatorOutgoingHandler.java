package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundPlaceGhostRecipePacket;

public class PlaceGhostRecipeSpectatorOutgoingHandler implements PacketHandler<ClientboundPlaceGhostRecipePacket, ServerConnection> {
    @Override
    public ClientboundPlaceGhostRecipePacket apply(ClientboundPlaceGhostRecipePacket packet, ServerConnection session) {
        return null;
    }
}
