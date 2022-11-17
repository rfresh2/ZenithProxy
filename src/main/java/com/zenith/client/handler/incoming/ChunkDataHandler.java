package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.zenith.client.ClientSession;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.Locale;

import static com.zenith.util.Constants.*;

public class ChunkDataHandler implements HandlerRegistry.AsyncIncomingHandler<ServerChunkDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerChunkDataPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().add(packet.getColumn());
        if (!CONFIG.client.server.address.toLowerCase(Locale.ROOT).contains("2b2t.org")) {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.dispatch(new PlayerOnlineEvent());
            }
        }
        return true;
    }

    @Override
    public Class<ServerChunkDataPacket> getPacketClass() {
        return ServerChunkDataPacket.class;
    }
}
