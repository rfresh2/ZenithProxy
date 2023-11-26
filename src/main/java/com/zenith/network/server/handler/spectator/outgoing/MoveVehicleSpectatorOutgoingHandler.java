package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class MoveVehicleSpectatorOutgoingHandler implements PacketHandler<ClientboundMoveVehiclePacket, ServerConnection> {
    @Override
    public ClientboundMoveVehiclePacket apply(ClientboundMoveVehiclePacket packet, ServerConnection session) {
        return null;
    }
}
