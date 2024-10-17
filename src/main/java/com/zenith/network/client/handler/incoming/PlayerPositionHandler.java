package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.module.impl.AntiAFK;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import com.zenith.network.server.ServerSession;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class PlayerPositionHandler implements ClientEventLoopPacketHandler<ClientboundPlayerPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerPositionPacket packet, @NonNull ClientSession session) {
        PlayerCache cache = CACHE.getPlayerCache();
        var teleportQueue = cache.getTeleportQueue();
        cache.getTeleportQueue().enqueue(packet.getId());
        while (teleportQueue.size() > 100) {
            var id = teleportQueue.dequeueInt();
            CLIENT_LOG.debug("Teleport queue larger than 100, dropping oldest entry. Dropped teleport: {} Last teleport: {}", id, packet.getId());
        }
        cache
            .setX((packet.getRelatives().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
            .setY((packet.getRelatives().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
            .setZ((packet.getRelatives().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
            .setVelX((packet.getRelatives().contains(PositionElement.DELTA_X) ? cache.getVelX() : 0.0d) + packet.getDeltaX())
            .setVelY((packet.getRelatives().contains(PositionElement.DELTA_Y) ? cache.getVelY() : 0.0d) + packet.getDeltaY())
            .setVelZ((packet.getRelatives().contains(PositionElement.DELTA_Z) ? cache.getVelZ() : 0.0d) + packet.getDeltaZ())
            .setYaw((packet.getRelatives().contains(PositionElement.Y_ROT) ? cache.getYaw() : 0.0f) + packet.getYaw())
            .setPitch((packet.getRelatives().contains(PositionElement.X_ROT) ? cache.getPitch() : 0.0f) + packet.getPitch());
        ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (isNull(currentPlayer) || !currentPlayer.isLoggedIn()) {
            MODULE.get(PlayerSimulation.class).handlePlayerPosRotate(packet.getId());
        } // else send to active player
        SpectatorSync.syncPlayerPositionWithSpectators();
        MODULE.get(AntiAFK.class).handlePlayerPosRotate();
        return true;
    }
}
