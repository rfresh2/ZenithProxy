package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class BlockEntityDataHandler implements AsyncIncomingHandler<ClientboundBlockEntityDataPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundBlockEntityDataPacket packet, @NonNull ClientSession session) {
        if (!CACHE.getChunkCache().updateTileEntity(packet)) {
            CLIENT_LOG.warn("Received ServerUpdateTileEntityPacket for chunk column that does not exist: {}, data: {}", packet.getPosition(), packet.getNbt());
            return false;
        }
        return true;
    }

    @Override
    public Class<ClientboundBlockEntityDataPacket> getPacketClass() {
        return ClientboundBlockEntityDataPacket.class;
    }
}
