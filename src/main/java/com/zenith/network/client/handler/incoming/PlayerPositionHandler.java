package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.module.impl.AntiAFK;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.MODULE_MANAGER;
import static java.util.Objects.isNull;

public class PlayerPositionHandler implements AsyncIncomingHandler<ClientboundPlayerPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerPositionPacket packet, @NonNull ClientSession session) {
        PlayerCache cache = CACHE.getPlayerCache();
        cache
                .setX((packet.getRelative().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
                .setY((packet.getRelative().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
                .setZ((packet.getRelative().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
                .setYaw((packet.getRelative().contains(PositionElement.YAW) ? cache.getYaw() : 0.0f) + packet.getYaw())
                .setPitch((packet.getRelative().contains(PositionElement.PITCH) ? cache.getPitch() : 0.0f) + packet.getPitch());
        ServerConnection currentPlayer = session.getProxy().getCurrentPlayer().get();
        if (isNull(currentPlayer)) {
            MODULE_MANAGER.get(PlayerSimulation.class).handlePlayerPosRotate(packet.getTeleportId());
        } else if (!currentPlayer.isLoggedIn()) { // possible race condition during login where we get a position packet before we're fully logged in
            Proxy.getInstance().getClient().send(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));
            Proxy.getInstance().getClient().send(new ServerboundMovePlayerPosRotPacket(false, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch()));
        } // else send to active player
        SpectatorUtils.syncPlayerPositionWithSpectators();
        MODULE_MANAGER.getModule(AntiAFK.class)
                .ifPresent(AntiAFK::handlePlayerPosRotate);
        return true;
    }
}
