package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class MapDataHandler implements AsyncIncomingHandler<ServerMapDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerMapDataPacket packet, ClientSession session) {
        CACHE.getMapDataCache().upsert(packet);
        return true;
    }

    @Override
    public Class<ServerMapDataPacket> getPacketClass() {
        return ServerMapDataPacket.class;
    }
}
