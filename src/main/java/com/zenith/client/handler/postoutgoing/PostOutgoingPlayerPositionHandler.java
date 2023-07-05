package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionHandler implements HandlerRegistry.PostOutgoingHandler<ClientPlayerPositionPacket, ClientSession> {

    @Override
    public void accept(ClientPlayerPositionPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorHelper.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerPositionPacket> getPacketClass() {
        return ClientPlayerPositionPacket.class;
    }
}
