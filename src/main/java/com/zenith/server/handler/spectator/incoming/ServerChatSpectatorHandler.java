package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerChatSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, ServerConnection> {

    @Override
    public boolean apply(ClientChatPacket packet, ServerConnection session) {
        if (packet.getMessage().startsWith("!m")) {
            session.getProxy().getClient().send(new ClientChatPacket(packet.getMessage().substring(2).trim()));
        } else {
            session.getProxy().getServerConnections().forEach(connection -> {
                connection.send(new ServerChatPacket("§c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage() + "§r", true));
            });
        }
        return false;
    }

    @Override
    public Class<ClientChatPacket> getPacketClass() {
        return ClientChatPacket.class;
    }
}
