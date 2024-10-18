package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookRemovePacket;

import static com.zenith.Shared.CACHE;

public class RecipeBookRemoveHandler implements ClientEventLoopPacketHandler<ClientboundRecipeBookRemovePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundRecipeBookRemovePacket packet, final ClientSession session) {
        CACHE.getRecipeCache().removeRecipeBookEntries(packet);
        return true;
    }
}
