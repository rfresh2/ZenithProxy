package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class UnlockRecipesHandler implements HandlerRegistry.IncomingHandler<ServerUnlockRecipesPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerUnlockRecipesPacket packet, @NonNull PorkClientSession session) {
        CACHE.getStatsCache()
                .setActivateFiltering(packet.getActivateFiltering())
                .setOpenCraftingBook(packet.getOpenCraftingBook());

        switch (packet.getAction()) {
            case INIT:
                CLIENT_LOG.info("Init recipes: recipes=%d, known=%d", packet.getRecipes().size(), packet.getAlreadyKnownRecipes().size());
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
