package com.zenith.network.client.handler.postoutgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingPlayerCommandHandler implements ClientEventLoopPacketHandler<ServerboundPlayerCommandPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundPlayerCommandPacket packet, final ClientSession session) {
        if (packet.getEntityId() != CACHE.getPlayerCache().getEntityId()) return true;
        switch (packet.getState()) {
            case START_SNEAKING -> {
                CACHE.getPlayerCache().setSneaking(true);
                SpectatorSync.sendPlayerSneakStatus();
            }
            case STOP_SNEAKING -> {
                CACHE.getPlayerCache().setSneaking(false);
                SpectatorSync.sendPlayerSneakStatus();
            }
            case START_SPRINTING -> CACHE.getPlayerCache().setSprinting(true);
            case STOP_SPRINTING -> CACHE.getPlayerCache().setSprinting(false);
        }
        return true;
    }
}
