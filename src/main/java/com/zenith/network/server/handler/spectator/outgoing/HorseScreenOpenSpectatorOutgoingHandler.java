package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundHorseScreenOpenPacket;

public class HorseScreenOpenSpectatorOutgoingHandler implements PacketHandler<ClientboundHorseScreenOpenPacket, ServerConnection> {
    @Override
    public ClientboundHorseScreenOpenPacket apply(ClientboundHorseScreenOpenPacket packet, ServerConnection session) {
        return null;
    }
}
