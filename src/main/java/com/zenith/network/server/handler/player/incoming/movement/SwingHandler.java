package com.zenith.network.server.handler.player.incoming.movement;

import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundAnimatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.zenith.Proxy;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class SwingHandler implements AsyncIncomingHandler<ServerboundSwingPacket, ServerConnection> {
    @Override
    public boolean applyAsync(ServerboundSwingPacket packet, ServerConnection session) {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            connection.sendAsync(new ClientboundAnimatePacket(
                    CACHE.getPlayerCache().getEntityId(),
                    Animation.SWING_ARM
            ));
        });
        return true;
    }
}
