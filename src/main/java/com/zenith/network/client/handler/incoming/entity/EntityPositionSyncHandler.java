package com.zenith.network.client.handler.incoming.entity;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;

import static com.zenith.Shared.CACHE;

public class EntityPositionSyncHandler implements ClientEventLoopPacketHandler<ClientboundEntityPositionSyncPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundEntityPositionSyncPacket packet, final ClientSession session) {
        var entity = CACHE.getEntityCache().get(packet.getId());
        if (entity == null) return false;
        entity.setX(packet.getX());
        entity.setY(packet.getY());
        entity.setZ(packet.getZ());
        entity.setYaw(packet.getYaw());
        entity.setPitch(packet.getPitch());
        entity.setVelX(packet.getDeltaX());
        entity.setVelY(packet.getDeltaY());
        entity.setVelZ(packet.getDeltaZ());
        // todo: onground
        return true;
    }
}
