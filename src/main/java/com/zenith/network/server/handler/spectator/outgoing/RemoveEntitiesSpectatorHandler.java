package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;

import java.util.Arrays;

public class RemoveEntitiesSpectatorHandler implements PacketHandler<ClientboundRemoveEntitiesPacket, ServerSession> {
    @Override
    public ClientboundRemoveEntitiesPacket apply(final ClientboundRemoveEntitiesPacket packet, final ServerSession session) {
        var cameraTarget = session.getCameraTarget();
        if (cameraTarget != null) {
            var camEntityId = cameraTarget.getEntityId();
            if (Arrays.stream(packet.getEntityIds()).anyMatch(id -> id == camEntityId)) {
                session.setCameraTarget(null);
                session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
                SpectatorSync.syncSpectatorPositionToEntity(session, cameraTarget);
            }
        }
        return packet;
    }
}
