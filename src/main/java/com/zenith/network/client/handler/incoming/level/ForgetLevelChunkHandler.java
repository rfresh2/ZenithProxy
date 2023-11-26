package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class ForgetLevelChunkHandler implements AsyncPacketHandler<ClientboundForgetLevelChunkPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundForgetLevelChunkPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().remove(packet.getX(), packet.getZ());
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            final int spectX = (int) connection.getSpectatorPlayerCache().getX() >> 4;
            final int spectZ = (int) connection.getSpectatorPlayerCache().getZ() >> 4;
            if ((spectX == packet.getX() || spectX + 1 == packet.getX() || spectX - 1 == packet.getX())
                    && (spectZ == packet.getZ() || spectZ + 1 == packet.getZ() || spectZ - 1 == packet.getZ())) {
                SpectatorUtils.syncSpectatorPositionToProxiedPlayer(connection);
            }
        });
        return true;
    }
}
