package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSpectatorActionPacket;

import static com.zenith.Globals.CACHE;

public class SpectateEntityHandler implements PacketHandler<ServerboundSpectatorActionPacket, ServerSession> {
    @Override
    public ServerboundSpectatorActionPacket apply(final ServerboundSpectatorActionPacket packet, final ServerSession session) {
        var entityIdOptional = packet.getEntityId();
        if (entityIdOptional.isPresent()) {
            var entityId = entityIdOptional.getAsInt();
            final Entity entity = CACHE.getEntityCache().get(entityId);
            if (entity != null) {
                session.setCameraTarget(entity);
                session.send(new ClientboundSetCameraPacket(entityId));
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                }
            }
        }
        return null;
    }
}
