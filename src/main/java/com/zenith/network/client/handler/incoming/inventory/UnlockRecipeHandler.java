package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipePacket;

import static com.zenith.Shared.CACHE;

public class UnlockRecipeHandler implements ClientEventLoopPacketHandler<ClientboundRecipePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundRecipePacket packet, final ClientSession session) {
        CACHE.getRecipeCache().updateUnlockedRecipes(packet);
        return true;
    }
}
