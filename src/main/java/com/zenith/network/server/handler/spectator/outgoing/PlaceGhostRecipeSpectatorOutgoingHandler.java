package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundPlaceGhostRecipePacket;

public class PlaceGhostRecipeSpectatorOutgoingHandler implements PacketHandler<ClientboundPlaceGhostRecipePacket, ServerSession> {
    @Override
    public ClientboundPlaceGhostRecipePacket apply(ClientboundPlaceGhostRecipePacket packet, ServerSession session) {
        return null;
    }
}
