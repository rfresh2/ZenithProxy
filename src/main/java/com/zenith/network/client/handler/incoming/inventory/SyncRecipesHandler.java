package com.zenith.network.client.handler.incoming.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SyncRecipesHandler implements ClientEventLoopPacketHandler<ClientboundUpdateRecipesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundUpdateRecipesPacket packet, @NonNull ClientSession session) {
        CACHE.getRecipeCache().setRecipeRegistry(packet);
        return true;
    }
}
