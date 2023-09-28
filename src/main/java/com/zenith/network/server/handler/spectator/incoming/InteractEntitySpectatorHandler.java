package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.zenith.Proxy;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class InteractEntitySpectatorHandler implements IncomingHandler<ServerboundInteractPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundInteractPacket packet, final ServerConnection session) {
        if (packet.getEntityId() == CACHE.getPlayerCache().getEntityId() && packet.getAction() == InteractAction.ATTACK) {
            session.setPlayerCam(true);
            session.send(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
            Proxy.getInstance().getActiveConnections().forEach(connection -> {
                connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
            });
        }
        return false;
    }
}
