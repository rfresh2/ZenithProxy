package com.zenith.network.client.handler.incoming.level;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class BlockEntityDataHandler implements ClientEventLoopPacketHandler<ClientboundBlockEntityDataPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundBlockEntityDataPacket packet, @NonNull ClientSession session) {
        if (!CACHE.getChunkCache().updateBlockEntity(packet)) {
            CLIENT_LOG.warn("Received ServerUpdateTileEntityPacket for chunk column that does not exist: [{}, {}, {}], data: {}", packet.getX(), packet.getY(), packet.getZ(), packet.getNbt());
            return false;
        }
        return true;
    }
}
