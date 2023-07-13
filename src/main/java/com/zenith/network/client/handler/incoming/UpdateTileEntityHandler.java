package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class UpdateTileEntityHandler implements AsyncIncomingHandler<ServerUpdateTileEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ServerUpdateTileEntityPacket packet, @NonNull ClientSession session) {
        if (!CACHE.getChunkCache().updateTileEntity(packet)) {
            CLIENT_LOG.warn("Received ServerUpdateTileEntityPacket for chunk column that does not exist: {}, data: {}", packet.getPosition(), packet.getNBT());
            return false;
        }
        return true;
    }

    @Override
    public Class<ServerUpdateTileEntityPacket> getPacketClass() {
        return ServerUpdateTileEntityPacket.class;
    }
}
