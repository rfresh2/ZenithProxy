package com.zenith.network.server.handler.player.incoming.movement;

import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundAnimatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class PlayerSwingArmPacketHandler implements AsyncIncomingHandler<ServerboundSwingPacket, ServerConnection> {
    @Override
    public boolean applyAsync(ServerboundSwingPacket packet, ServerConnection session) {
        session.getProxy().getSpectatorConnections().forEach(connection -> {
            connection.send(new ClientboundAnimatePacket(
                    CACHE.getPlayerCache().getEntityId(),
                    Animation.SWING_ARM
            ));
        });
        return true;
    }

    @Override
    public Class<ServerboundSwingPacket> getPacketClass() {
        return ServerboundSwingPacket.class;
    }
}
