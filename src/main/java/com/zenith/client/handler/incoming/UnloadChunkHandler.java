package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.zenith.Proxy;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class UnloadChunkHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUnloadChunkPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerUnloadChunkPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().remove(packet.getX(), packet.getZ());
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            final int spectX = (int) connection.getSpectatorPlayerCache().getX() >> 4;
            final int spectZ = (int) connection.getSpectatorPlayerCache().getZ() >> 4;
            if ((spectX == packet.getX() || spectX + 1 == packet.getX() || spectX - 1 == packet.getX())
                    && (spectZ == packet.getZ() || spectZ + 1 == packet.getZ() || spectZ - 1 == packet.getZ())) {
                SpectatorHelper.syncSpectatorPositionToPlayer(connection);
            }
        });
        return true;
    }

    @Override
    public Class<ServerUnloadChunkPacket> getPacketClass() {
        return ServerUnloadChunkPacket.class;
    }
}
