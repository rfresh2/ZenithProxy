package com.zenith.network.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.registry.PostOutgoingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class ClientRequestPacketPostHandler implements PostOutgoingHandler<ClientRequestPacket, ServerConnection> {
    @Override
    public void accept(ClientRequestPacket packet, ServerConnection session) {
        if (packet.getRequest() == ClientRequest.RESPAWN) {
            CACHE.getPlayerCache().getThePlayer().setHealth(20.0f);
            try {
                ((EntityPlayer) CACHE.getEntityCache().get(CACHE.getPlayerCache().getEntityId())).setHealth(20.0f);
            } catch (final Throwable e) {
                // do nothing
            }
        }
    }

    @Override
    public Class<ClientRequestPacket> getPacketClass() {
        return ClientRequestPacket.class;
    }
}
