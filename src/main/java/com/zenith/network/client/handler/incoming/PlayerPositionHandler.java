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

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.MODULE;
import static java.util.Objects.isNull;

public class PlayerPositionHandler implements ClientEventLoopPacketHandler<ClientboundPlayerPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerPositionPacket packet, @NonNull ClientSession session) {
        PlayerCache cache = CACHE.getPlayerCache();
        cache.getTeleportQueue().enqueue(packet.getTeleportId());
        cache
                .setX((packet.getRelative().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
                .setY((packet.getRelative().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
                .setZ((packet.getRelative().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
                .setYaw((packet.getRelative().contains(PositionElement.YAW) ? cache.getYaw() : 0.0f) + packet.getYaw())
                .setPitch((packet.getRelative().contains(PositionElement.PITCH) ? cache.getPitch() : 0.0f) + packet.getPitch());
        ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (isNull(currentPlayer) || !currentPlayer.isLoggedIn()) {
            MODULE.get(PlayerSimulation.class).handlePlayerPosRotate(packet.getTeleportId());
        } // else send to active player
        SpectatorSync.syncPlayerPositionWithSpectators();
        MODULE.get(AntiAFK.class).handlePlayerPosRotate();
        return true;
    }
}
