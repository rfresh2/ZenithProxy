package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookAddPacket;

import static com.zenith.Shared.CACHE;

public class RecipeBookAddHandler implements ClientEventLoopPacketHandler<ClientboundRecipeBookAddPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundRecipeBookAddPacket packet, final ClientSession session) {
        CACHE.getRecipeCache().addRecipeBookEntries(packet);
        return true;
    }
}
