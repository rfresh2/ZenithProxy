package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerRotationHandler implements PostOutgoingHandler<ServerboundMovePlayerRotPacket, ClientSession> {
    @Override
    public void accept(ServerboundMovePlayerRotPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorUtils.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ServerboundMovePlayerRotPacket> getPacketClass() {
        return ServerboundMovePlayerRotPacket.class;
    }
}
