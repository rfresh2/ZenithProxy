package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class UpdateTileEntityHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUpdateTileEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ServerUpdateTileEntityPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().updateTileEntity(packet);
        return true;
    }

    @Override
    public Class<ServerUpdateTileEntityPacket> getPacketClass() {
        return ServerUpdateTileEntityPacket.class;
    }
}
