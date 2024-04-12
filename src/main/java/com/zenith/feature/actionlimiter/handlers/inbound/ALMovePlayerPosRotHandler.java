package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;

import static com.zenith.Shared.CONFIG;

public class ALMovePlayerPosRotHandler implements PacketHandler<ServerboundMovePlayerPosRotPacket, ServerConnection> {
    @Override
    public ServerboundMovePlayerPosRotPacket apply(final ServerboundMovePlayerPosRotPacket packet, final ServerConnection session) {
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
