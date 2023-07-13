package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerRotationHandler implements PostOutgoingHandler<ClientPlayerRotationPacket, ClientSession> {
    @Override
    public void accept(ClientPlayerRotationPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        SpectatorUtils.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerRotationPacket> getPacketClass() {
        return ClientPlayerRotationPacket.class;
    }
}
