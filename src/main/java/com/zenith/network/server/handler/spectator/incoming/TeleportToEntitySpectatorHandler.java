package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;

import static com.zenith.Shared.CACHE;

public class TeleportToEntitySpectatorHandler implements PacketHandler<ServerboundTeleportToEntityPacket, ServerConnection> {
    @Override
    public ServerboundTeleportToEntityPacket apply(final ServerboundTeleportToEntityPacket packet, final ServerConnection session) {
        final Entity targetEntity = CACHE.getEntityCache().get(packet.getTarget());
        if (targetEntity != null) {
            if (session.hasCameraTarget()) {
                session.setCameraTarget(null);
                session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
            }
            SpectatorSync.syncSpectatorPositionToEntity(session, targetEntity);
        }
        return null;
    }
}
