package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class UnlockRecipesHandler implements AsyncIncomingHandler<ServerUnlockRecipesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerUnlockRecipesPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache()
                .setActivateFiltering(packet.getActivateFiltering())
                .setOpenCraftingBook(packet.getOpenCraftingBook());

        switch (packet.getAction()) {
            case INIT:
                CLIENT_LOG.debug("Init recipes: recipes={}, known={}", packet.getRecipes().size(), packet.getAlreadyKnownRecipes().size());
                CACHE.getStatsCache().getRecipes().addAll(packet.getRecipes());
                CACHE.getStatsCache().getAlreadyKnownRecipes().addAll(packet.getAlreadyKnownRecipes());
                break;
            case ADD:
                CACHE.getStatsCache().getAlreadyKnownRecipes().addAll(packet.getRecipes());
                break;
            case REMOVE:
                CACHE.getStatsCache().getAlreadyKnownRecipes().removeAll(packet.getRecipes());
                break;
        }
        return true;
    }

    @Override
    public Class<ServerUnlockRecipesPacket> getPacketClass() {
        return ServerUnlockRecipesPacket.class;
    }
}
