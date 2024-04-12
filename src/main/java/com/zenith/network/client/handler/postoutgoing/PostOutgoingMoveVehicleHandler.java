package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingMoveVehicleHandler implements ClientEventLoopPacketHandler<ServerboundMoveVehiclePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundMoveVehiclePacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setX(packet.getX());
        CACHE.getPlayerCache().setY(packet.getY());
        CACHE.getPlayerCache().setZ(packet.getZ());
        return true;
    }
}
