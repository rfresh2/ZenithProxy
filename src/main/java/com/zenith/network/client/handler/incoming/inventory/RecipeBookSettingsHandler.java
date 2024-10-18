package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookSettingsPacket;

import static com.zenith.Shared.CACHE;

public class RecipeBookSettingsHandler implements ClientEventLoopPacketHandler<ClientboundRecipeBookSettingsPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundRecipeBookSettingsPacket packet, final ClientSession session) {
        CACHE.getRecipeCache().setRecipeBookSettings(packet);
        return true;
    }
}
