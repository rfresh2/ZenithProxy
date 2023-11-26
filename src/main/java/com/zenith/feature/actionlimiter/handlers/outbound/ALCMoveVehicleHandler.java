package com.zenith.feature.actionlimiter.handlers.outbound;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MathHelper;

import static com.zenith.Shared.CONFIG;

public class ALCMoveVehicleHandler implements PacketHandler<ClientboundMoveVehiclePacket, ServerConnection> {
    @Override
    public ClientboundMoveVehiclePacket apply(final ClientboundMoveVehiclePacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowMovement)
            return packet;
        if (packet.getY() <= CONFIG.client.extra.actionLimiter.movementMinY) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        if (MathHelper.distance2d(CONFIG.client.extra.actionLimiter.movementHomeX,
                                  CONFIG.client.extra.actionLimiter.movementHomeZ,
                                  packet.getX(),
                                  packet.getZ()) > CONFIG.client.extra.actionLimiter.movementDistance) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        return packet;
    }
}
