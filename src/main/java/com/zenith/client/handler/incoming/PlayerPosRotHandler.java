package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.zenith.cache.data.PlayerCache;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static java.util.Objects.isNull;

public class PlayerPosRotHandler implements HandlerRegistry.AsyncIncomingHandler<ServerPlayerPositionRotationPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerPlayerPositionRotationPacket packet, @NonNull ClientSession session) {
        PlayerCache cache = CACHE.getPlayerCache();
        cache
                .setX((packet.getRelativeElements().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
                .setY((packet.getRelativeElements().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
                .setZ((packet.getRelativeElements().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
                .setYaw((packet.getRelativeElements().contains(PositionElement.YAW) ? cache.getYaw() : 0.0f) + packet.getYaw())
                .setPitch((packet.getRelativeElements().contains(PositionElement.PITCH) ? cache.getPitch() : 0.0f) + packet.getPitch());
        if (isNull(session.getProxy().getCurrentPlayer().get())) {
            session.send(new ClientTeleportConfirmPacket(packet.getTeleportId()));
            session.send(new ClientPlayerPositionRotationPacket(
                    false,
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch()
            ));
        }
        SpectatorHelper.syncPlayerPositionWithSpectators();
        return true;
    }

    @Override
    public Class<ServerPlayerPositionRotationPacket> getPacketClass() {
        return ServerPlayerPositionRotationPacket.class;
    }
}
