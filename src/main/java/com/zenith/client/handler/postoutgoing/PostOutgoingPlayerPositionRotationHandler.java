package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionRotationHandler implements HandlerRegistry.PostOutgoingHandler<ClientPlayerPositionRotationPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerPositionRotationPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        SpectatorHelper.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerPositionRotationPacket> getPacketClass() {
        return ClientPlayerPositionRotationPacket.class;
    }
}
