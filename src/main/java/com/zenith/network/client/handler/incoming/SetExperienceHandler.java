package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class SetExperienceHandler implements AsyncIncomingHandler<ClientboundSetExperiencePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSetExperiencePacket packet, ClientSession session) {
        CACHE.getPlayerCache().getThePlayer()
                .setTotalExperience(packet.getTotalExperience())
                .setLevel(packet.getLevel())
                .setExperience(packet.getExperience());
        return true;
    }

    @Override
    public Class<ClientboundSetExperiencePacket> getPacketClass() {
        return ClientboundSetExperiencePacket.class;
    }
}
