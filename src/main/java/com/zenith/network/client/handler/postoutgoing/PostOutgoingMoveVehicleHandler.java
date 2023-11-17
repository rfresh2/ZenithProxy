package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingAsyncHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingMoveVehicleHandler implements PostOutgoingAsyncHandler<ServerboundMoveVehiclePacket, ClientSession> {
    @Override
    public void acceptAsync(final ServerboundMoveVehiclePacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setX(packet.getX());
        CACHE.getPlayerCache().setY(packet.getY());
        CACHE.getPlayerCache().setZ(packet.getZ());
    }
}
