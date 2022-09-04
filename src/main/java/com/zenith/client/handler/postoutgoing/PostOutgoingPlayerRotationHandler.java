package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;

import static com.zenith.util.Constants.CACHE;

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
