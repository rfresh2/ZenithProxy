package com.zenith.network.client.handler.postoutgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerPositionHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerPosPacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerboundMovePlayerPosPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorSync.syncPlayerPositionWithSpectators();
        return true;
    }
}
