package com.zenith.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.zenith.Proxy;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.server.ServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;

import static com.zenith.util.Constants.CACHE;

public class JoinGamePostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, ServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull ServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));

        //send cached data
        DataCache.sendCacheData(CACHE.getAllData(), session);

        // init any active spectators
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .filter(connection -> !connection.isPlayerCam())
                .forEach(connection -> {
                    session.send(connection.getEntitySpawnPacket());
                    session.send(connection.getEntityMetadataPacket());
                });

        session.setLoggedIn(true); // allows server packets to start being send to player

        // ensure we actually fully sync chunks if player is immediately connecting on proxy connect
        // avoids race conditions especially when proxy exits prio q right as player joins
        if (Proxy.getInstance().getConnectTime().isAfter(Instant.now().minus(Duration.ofSeconds(30)))) {
            ChunkCache.sync();
        }
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
