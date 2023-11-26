package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundAnimatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingSwingHandler implements AsyncPacketHandler<ServerboundSwingPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundSwingPacket packet, final ClientSession session) {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            connection.sendAsync(new ClientboundAnimatePacket(
                CACHE.getPlayerCache().getEntityId(),
                Animation.SWING_ARM
            ));
        });
        return true;
    }
}
