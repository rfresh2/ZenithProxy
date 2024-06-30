package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;

import static com.zenith.Shared.CACHE;

public class InteractEntitySpectatorHandler implements PacketHandler<ServerboundInteractPacket, ServerSession> {
    @Override
    public ServerboundInteractPacket apply(final ServerboundInteractPacket packet, final ServerSession session) {
        if (packet.getAction() == InteractAction.ATTACK) {
            final Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                session.setCameraTarget(entity);
                session.send(new ClientboundSetCameraPacket(packet.getEntityId()));
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
