package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MathHelper;

import static com.zenith.Shared.CONFIG;

public class ALMoveVehicleHandler implements IncomingHandler<ServerboundMoveVehiclePacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundMoveVehiclePacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowMovement)
            return true;
        if (packet.getY() <= CONFIG.client.extra.actionLimiter.movementMinY) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return false;
        }
        if (MathHelper.distance2d(CONFIG.client.extra.actionLimiter.movementHomeX,
                                  CONFIG.client.extra.actionLimiter.movementHomeZ,
                                  packet.getX(),
                                  packet.getZ()) > CONFIG.client.extra.actionLimiter.movementDistance) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return false;
        }
        return true;
    }
}
