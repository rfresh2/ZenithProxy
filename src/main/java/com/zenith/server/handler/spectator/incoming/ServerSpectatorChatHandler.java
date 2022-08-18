package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class ServerSpectatorChatHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, PorkServerConnection> {

    @Override
    public boolean apply(@NonNull ClientChatPacket packet, @NonNull PorkServerConnection session) {
        // todo: handle some commands spectators can run
        return true;
    }

    @Override
    public Class<ClientChatPacket> getPacketClass() {
        return ClientChatPacket.class;
    }
}
