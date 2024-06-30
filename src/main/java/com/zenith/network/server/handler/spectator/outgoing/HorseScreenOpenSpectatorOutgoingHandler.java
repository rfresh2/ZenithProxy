package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundHorseScreenOpenPacket;

public class HorseScreenOpenSpectatorOutgoingHandler implements PacketHandler<ClientboundHorseScreenOpenPacket, ServerSession> {
    @Override
    public ClientboundHorseScreenOpenPacket apply(ClientboundHorseScreenOpenPacket packet, ServerSession session) {
        return null;
    }
}
