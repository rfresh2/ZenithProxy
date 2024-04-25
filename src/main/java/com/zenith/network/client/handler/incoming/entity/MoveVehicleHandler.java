package com.zenith.network.client.handler.incoming.entity;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;

import static com.zenith.Shared.CACHE;

public class MoveVehicleHandler implements ClientEventLoopPacketHandler<ClientboundMoveVehiclePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundMoveVehiclePacket packet, final ClientSession session) {
        // not sure if player pos will be slightly offset from vehicle's
        CACHE.getPlayerCache().setX(packet.getX());
        CACHE.getPlayerCache().setY(packet.getY());
        CACHE.getPlayerCache().setZ(packet.getZ());
        return true;
    }
}
