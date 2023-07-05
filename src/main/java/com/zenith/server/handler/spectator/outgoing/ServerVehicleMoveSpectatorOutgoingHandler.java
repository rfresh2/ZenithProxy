package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerVehicleMovePacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerVehicleMoveSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerVehicleMovePacket, ServerConnection> {
    @Override
    public ServerVehicleMovePacket apply(ServerVehicleMovePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerVehicleMovePacket> getPacketClass() {
        return ServerVehicleMovePacket.class;
    }
}
