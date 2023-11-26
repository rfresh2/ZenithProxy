package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerRotationHandler implements AsyncPacketHandler<ServerboundMovePlayerRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerboundMovePlayerRotPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorUtils.syncPlayerPositionWithSpectators();
//        CLIENT_LOG.info("Client set player rotation: {}, {}", packet.getYaw(), packet.getPitch());
        return true;
    }
}
