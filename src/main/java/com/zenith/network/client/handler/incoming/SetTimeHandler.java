package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.zenith.cache.data.chunk.WorldTimeData;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.TPS_CALCULATOR;

public class SetTimeHandler implements AsyncPacketHandler<ClientboundSetTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundSetTimePacket packet, ClientSession session) {
        CACHE.getChunkCache().setWorldTimeData(new WorldTimeData(packet));
        TPS_CALCULATOR.handleTimeUpdate(packet);
        return true;
    }
}
