package com.zenith.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;

public class ClientRequestPacketPostHandler implements HandlerRegistry.PostOutgoingHandler<ClientRequestPacket, ServerConnection> {
    @Override
    public void accept(ClientRequestPacket packet, ServerConnection session) {
        if (packet.getRequest() == ClientRequest.RESPAWN) {
            CACHE.getPlayerCache().getThePlayer().setHealth(20.0f);
        }
    }

    @Override
    public Class<ClientRequestPacket> getPacketClass() {
        return ClientRequestPacket.class;
    }
}
