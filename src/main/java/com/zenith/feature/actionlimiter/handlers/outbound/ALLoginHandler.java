package com.zenith.feature.actionlimiter.handlers.outbound;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MathHelper;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;

public class ALLoginHandler implements PacketHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public ClientboundLoginPacket apply(final ClientboundLoginPacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowMovement)
            return packet;
        int playerX = (int) CACHE.getPlayerCache().getX();
        int playerY = (int) CACHE.getPlayerCache().getY();
        int playerZ = (int) CACHE.getPlayerCache().getZ();
        if (playerY <= CONFIG.client.extra.actionLimiter.movementMinY) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        if (MathHelper.distance2d(CONFIG.client.extra.actionLimiter.movementHomeX,
                                  CONFIG.client.extra.actionLimiter.movementHomeZ,
                                  playerX,
                                  playerZ) > CONFIG.client.extra.actionLimiter.movementDistance) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        return packet;
    }
}
