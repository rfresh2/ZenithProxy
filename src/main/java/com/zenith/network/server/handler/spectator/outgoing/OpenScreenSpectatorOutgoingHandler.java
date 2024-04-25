package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

public class OpenScreenSpectatorOutgoingHandler implements PacketHandler<ClientboundOpenScreenPacket, ServerConnection> {
    @Override
    public ClientboundOpenScreenPacket apply(ClientboundOpenScreenPacket packet, ServerConnection session) {
        return null;
    }
}
