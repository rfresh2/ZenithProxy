package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerVehicleMovePacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerVehicleMoveSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerVehicleMovePacket, PorkServerConnection> {
    @Override
    public ServerVehicleMovePacket apply(ServerVehicleMovePacket packet, PorkServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerVehicleMovePacket> getPacketClass() {
        return ServerVehicleMovePacket.class;
    }
}
