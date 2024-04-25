package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;

public class SetHealthSpectatorOutgoingHandler implements PacketHandler<ClientboundSetHealthPacket, ServerConnection> {
    @Override
    public ClientboundSetHealthPacket apply(ClientboundSetHealthPacket packet, ServerConnection session) {
        return null;
    }
}
