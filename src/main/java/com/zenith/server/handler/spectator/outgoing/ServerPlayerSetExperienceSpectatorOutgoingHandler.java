package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerPlayerSetExperienceSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerSetExperiencePacket, ServerConnection> {
    @Override
    public ServerPlayerSetExperiencePacket apply(ServerPlayerSetExperiencePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerSetExperiencePacket> getPacketClass() {
        return ServerPlayerSetExperiencePacket.class;
    }
}
