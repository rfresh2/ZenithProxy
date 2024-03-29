package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class InteractEntitySpectatorHandler implements PacketHandler<ServerboundInteractPacket, ServerConnection> {
    @Override
    public ServerboundInteractPacket apply(final ServerboundInteractPacket packet, final ServerConnection session) {
        if (packet.getAction() == InteractAction.ATTACK) {
            final Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                session.setCameraTarget(entity);
                session.send(new ClientboundSetCameraPacket(packet.getEntityId()));
                Proxy.getInstance().getActiveConnections().forEach(connection -> {
                    connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                });
            }
        }
        return null;
    }
}
