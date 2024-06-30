package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

public class OpenScreenSpectatorOutgoingHandler implements PacketHandler<ClientboundOpenScreenPacket, ServerSession> {
    @Override
    public ClientboundOpenScreenPacket apply(ClientboundOpenScreenPacket packet, ServerSession session) {
        return null;
    }
}
