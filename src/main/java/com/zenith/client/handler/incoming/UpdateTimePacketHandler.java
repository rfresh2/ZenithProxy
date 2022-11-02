package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;

public class UpdateTimePacketHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUpdateTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerUpdateTimePacket packet, ClientSession session) {
        session.getProxy().getTpsCalculator().handleTimeUpdate(packet);
        return true;
    }

    @Override
    public Class<ServerUpdateTimePacket> getPacketClass() {
        return ServerUpdateTimePacket.class;
    }
}
