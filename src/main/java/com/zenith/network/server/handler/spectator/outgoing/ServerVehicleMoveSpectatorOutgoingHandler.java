package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerVehicleMovePacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerVehicleMoveSpectatorOutgoingHandler implements OutgoingHandler<ServerVehicleMovePacket, ServerConnection> {
    @Override
    public ServerVehicleMovePacket apply(ServerVehicleMovePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerVehicleMovePacket> getPacketClass() {
        return ServerVehicleMovePacket.class;
    }
}
