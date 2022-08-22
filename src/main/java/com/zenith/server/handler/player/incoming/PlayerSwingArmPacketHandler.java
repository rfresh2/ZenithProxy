package com.zenith.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAnimationPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;

public class PlayerSwingArmPacketHandler implements HandlerRegistry.AsyncIncomingHandler<ClientPlayerSwingArmPacket, PorkServerConnection> {
    @Override
    public boolean applyAsync(ClientPlayerSwingArmPacket packet, PorkServerConnection session) {
        session.getProxy().getSpectatorConnections().forEach(connection -> {
            connection.send(new ServerEntityAnimationPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    Animation.SWING_ARM
            ));
        });
        return true;
    }

    @Override
    public Class<ClientPlayerSwingArmPacket> getPacketClass() {
        return null;
    }
}
