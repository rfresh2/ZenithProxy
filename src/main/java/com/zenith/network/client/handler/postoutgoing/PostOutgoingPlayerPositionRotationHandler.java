package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionRotationHandler implements PostOutgoingHandler<ClientPlayerPositionRotationPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerPositionRotationPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        SpectatorUtils.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerPositionRotationPacket> getPacketClass() {
        return ClientPlayerPositionRotationPacket.class;
    }
}
