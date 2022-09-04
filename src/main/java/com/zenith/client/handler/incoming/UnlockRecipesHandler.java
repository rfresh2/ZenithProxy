package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class UnlockRecipesHandler implements HandlerRegistry.AsyncIncomingHandler<ServerUnlockRecipesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerUnlockRecipesPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache()
                .setActivateFiltering(packet.getActivateFiltering())
                .setOpenCraftingBook(packet.getOpenCraftingBook());

        switch (packet.getAction()) {
            case INIT:
                CLIENT_LOG.debug("Init recipes: recipes=%d, known=%d", packet.getRecipes().size(), packet.getAlreadyKnownRecipes().size());
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
