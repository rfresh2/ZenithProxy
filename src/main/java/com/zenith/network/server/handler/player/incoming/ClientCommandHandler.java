package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class ClientCommandHandler implements AsyncIncomingHandler<ServerboundClientCommandPacket, ServerConnection> {
    @Override
    public boolean applyAsync(ServerboundClientCommandPacket packet, ServerConnection session) {
        if (packet.getRequest() == ClientCommand.RESPAWN) {
            CACHE.getPlayerCache().getThePlayer().setHealth(20.0f);
            try {
                ((EntityPlayer) CACHE.getEntityCache().get(CACHE.getPlayerCache().getEntityId())).setHealth(20.0f);
            } catch (final Throwable e) {
                // do nothing
            }
        }
        return true;
    }
}
