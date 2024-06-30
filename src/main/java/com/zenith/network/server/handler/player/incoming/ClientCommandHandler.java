package com.zenith.network.server.handler.player.incoming;

import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;

import static com.zenith.Shared.CACHE;

public class ClientCommandHandler implements AsyncPacketHandler<ServerboundClientCommandPacket, ServerSession> {
    @Override
    public boolean applyAsync(ServerboundClientCommandPacket packet, ServerSession session) {
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
