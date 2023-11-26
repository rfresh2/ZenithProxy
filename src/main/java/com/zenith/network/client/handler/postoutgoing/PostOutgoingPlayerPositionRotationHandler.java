package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionRotationHandler implements AsyncPacketHandler<ServerboundMovePlayerPosRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerboundMovePlayerPosRotPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorUtils.syncPlayerPositionWithSpectators();
//        CLIENT_LOG.info("Client set player position rotation: {}, {}, {}, {}, {}", packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
        return true;
    }
}
