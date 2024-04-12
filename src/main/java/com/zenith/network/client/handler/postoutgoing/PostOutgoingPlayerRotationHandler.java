package com.zenith.network.client.handler.postoutgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerRotationHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerboundMovePlayerRotPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorSync.syncPlayerPositionWithSpectators();
//        CLIENT_LOG.info("Client set player rotation: {}, {}", packet.getYaw(), packet.getPitch());
        return true;
    }
}
