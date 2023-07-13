package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionHandler implements PostOutgoingHandler<ClientPlayerPositionPacket, ClientSession> {

    @Override
    public void accept(ClientPlayerPositionPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorUtils.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerPositionPacket> getPacketClass() {
        return ClientPlayerPositionPacket.class;
    }
}
