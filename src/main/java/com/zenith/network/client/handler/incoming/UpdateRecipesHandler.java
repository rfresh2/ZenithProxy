package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.Arrays;

import static com.zenith.Shared.CACHE;

public class UpdateRecipesHandler implements AsyncIncomingHandler<ClientboundUpdateRecipesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundUpdateRecipesPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache().getRecipes().clear();
        CACHE.getStatsCache().getRecipes().addAll(Arrays.asList(packet.getRecipes()));
        return true;
    }

    @Override
    public Class<ClientboundUpdateRecipesPacket> getPacketClass() {
        return ClientboundUpdateRecipesPacket.class;
    }
}
