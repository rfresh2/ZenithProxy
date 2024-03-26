package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;

public class MapDataHandler implements ClientEventLoopPacketHandler<ClientboundMapItemDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundMapItemDataPacket packet, ClientSession session) {
        CACHE.getMapDataCache().upsert(packet);
        return true;
    }
}
