package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class RespawnHandler implements HandlerRegistry.AsyncIncomingHandler<ServerRespawnPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerRespawnPacket packet, @NonNull ClientSession session) {
        if (CACHE.getPlayerCache().getDimension() != packet.getDimension()) {
            CACHE.reset(false);
            // only partial reset chunk and entity cache?
            disconnectSpectators(session, "Player changed dimensions");
        } else {
            disconnectSpectators(session, "Player respawned");
        }
        CACHE.getPlayerCache()
                .setDimension(packet.getDimension())
                .setGameMode(packet.getGameMode())
                .setWorldType(packet.getWorldType())
                .setDifficulty(packet.getDifficulty());

        return true;
    }

    @Override
    public Class<ServerRespawnPacket> getPacketClass() {
        return ServerRespawnPacket.class;
    }

    // todo: handle this situation without disconnecting spectators
    //  on next PlayerPositionRotation we need to spawn spectators back both on their side and current player side
    private void disconnectSpectators(ClientSession clientSession, final String reason)
    {
        clientSession.getProxy().getSpectatorConnections().forEach(connection -> {
            connection.disconnect(reason);
        });
    }
}
