package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

public class PluginMessageHandler implements IncomingHandler<ServerPluginMessagePacket, ClientSession> {
    @Override
    public boolean apply(ServerPluginMessagePacket packet, ClientSession session) {
        return !packet.getChannel().equalsIgnoreCase("MC|Brand");
    }

    @Override
    public Class<ServerPluginMessagePacket> getPacketClass() {
        return ServerPluginMessagePacket.class;
    }
}
