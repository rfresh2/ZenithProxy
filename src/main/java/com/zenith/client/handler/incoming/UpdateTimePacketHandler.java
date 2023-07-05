package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;

import static com.zenith.Shared.TPS_CALCULATOR;

public class UpdateTimePacketHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUpdateTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerUpdateTimePacket packet, ClientSession session) {
        TPS_CALCULATOR.handleTimeUpdate(packet);
        return true;
    }

    @Override
    public Class<ServerUpdateTimePacket> getPacketClass() {
        return ServerUpdateTimePacket.class;
    }
}
