package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPlayerSetExperienceSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerSetExperiencePacket, PorkServerConnection> {
    @Override
    public ServerPlayerSetExperiencePacket apply(ServerPlayerSetExperiencePacket packet, PorkServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerSetExperiencePacket> getPacketClass() {
        return ServerPlayerSetExperiencePacket.class;
    }
}
