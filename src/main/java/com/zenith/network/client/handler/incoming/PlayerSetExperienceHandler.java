package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class PlayerSetExperienceHandler implements AsyncIncomingHandler<ServerPlayerSetExperiencePacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerPlayerSetExperiencePacket packet, ClientSession session) {
        CACHE.getPlayerCache().getThePlayer()
                .setTotalExperience(packet.getTotalExperience())
                .setLevel(packet.getLevel())
                .setExperience(packet.getSlot());
        return true;
    }

    @Override
    public Class<ServerPlayerSetExperiencePacket> getPacketClass() {
        return ServerPlayerSetExperiencePacket.class;
    }
}
