package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetCursorItemPacket;

import static com.zenith.Shared.CACHE;

public class SetCursorItemHandler implements ClientEventLoopPacketHandler<ClientboundSetCursorItemPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetCursorItemPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getInventoryCache().setMouseStack(packet.getContents());
        return true;
    }
}
