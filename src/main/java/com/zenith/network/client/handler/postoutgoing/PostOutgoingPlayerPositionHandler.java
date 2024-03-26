package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerPosPacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerboundMovePlayerPosPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorSync.syncPlayerPositionWithSpectators();
//        CLIENT_LOG.info("Client set player position: {}, {}, {}", packet.getX(), packet.getY(), packet.getZ());
        return true;
    }
}
