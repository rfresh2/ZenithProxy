package com.zenith.network.client.handler.postoutgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;

import static com.zenith.Globals.CACHE;

public class PostOutgoingPlayerPositionRotationHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerPosRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerboundMovePlayerPosRotPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        SpectatorSync.syncPlayerPositionWithSpectators();
        if (!CACHE.getPlayerCache().isClientLoaded()) {
            session.send(ServerboundPlayerLoadedPacket.INSTANCE);
        }
        return true;
    }
}
