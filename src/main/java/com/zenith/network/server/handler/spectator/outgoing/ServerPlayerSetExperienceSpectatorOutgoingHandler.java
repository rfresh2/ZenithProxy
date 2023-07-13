package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerPlayerSetExperienceSpectatorOutgoingHandler implements OutgoingHandler<ServerPlayerSetExperiencePacket, ServerConnection> {
    @Override
    public ServerPlayerSetExperiencePacket apply(ServerPlayerSetExperiencePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerSetExperiencePacket> getPacketClass() {
        return ServerPlayerSetExperiencePacket.class;
    }
}
