package com.zenith.network.client.handler.incoming.entity;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;

import static com.zenith.Globals.CACHE;

public class MoveVehicleHandler implements ClientEventLoopPacketHandler<ClientboundMoveVehiclePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundMoveVehiclePacket packet, final ClientSession session) {
        var vehicleId = CACHE.getPlayerCache().getThePlayer().getVehicleId();
        var vehicleEntity = CACHE.getEntityCache().get(vehicleId);
        while (vehicleEntity != null && vehicleEntity.isInVehicle()) {
            vehicleEntity = CACHE.getEntityCache().get(vehicleEntity.getVehicleId());
        }
        if (vehicleEntity != null) {
            var lDist = MathHelper.distance3d(packet.getX(), packet.getY(), packet.getZ(), vehicleEntity.lerpTargetX(), vehicleEntity.lerpTargetY(), vehicleEntity.lerpTargetZ());
            if (lDist > 1.0E-5F) {
                vehicleEntity.cancelLerp();
                vehicleEntity.setX(packet.getX());
                vehicleEntity.setY(packet.getY());
                vehicleEntity.setZ(packet.getZ());
                vehicleEntity.setYaw(packet.getYaw());
                vehicleEntity.setPitch(packet.getPitch());
            }
        }
        return true;
    }
}
