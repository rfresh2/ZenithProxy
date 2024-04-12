package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;

public class SetCarriedItemSpectatorOutgoingHandler implements PacketHandler<ClientboundSetCarriedItemPacket, ServerConnection> {
    @Override
    public ClientboundSetCarriedItemPacket apply(ClientboundSetCarriedItemPacket packet, ServerConnection session) {
        return null;
    }
}
