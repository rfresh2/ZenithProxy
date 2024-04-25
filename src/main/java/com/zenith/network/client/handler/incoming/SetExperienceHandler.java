package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;

import static com.zenith.Shared.CACHE;

public class SetExperienceHandler implements ClientEventLoopPacketHandler<ClientboundSetExperiencePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSetExperiencePacket packet, ClientSession session) {
        CACHE.getPlayerCache().getThePlayer()
                .setTotalExperience(packet.getTotalExperience())
                .setLevel(packet.getLevel())
                .setExperience(packet.getExperience());
        return true;
    }
}
