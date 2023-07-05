package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerRotationHandler implements HandlerRegistry.PostOutgoingHandler<ClientPlayerRotationPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerRotationPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        SpectatorHelper.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerRotationPacket> getPacketClass() {
        return ClientPlayerRotationPacket.class;
    }
}
