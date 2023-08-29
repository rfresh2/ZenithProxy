package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ClientboundSetExperienceSpectatorOutgoingHandler implements OutgoingHandler<ClientboundSetExperiencePacket, ServerConnection> {
    @Override
    public ClientboundSetExperiencePacket apply(ClientboundSetExperiencePacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundSetExperiencePacket> getPacketClass() {
        return ClientboundSetExperiencePacket.class;
    }
}
