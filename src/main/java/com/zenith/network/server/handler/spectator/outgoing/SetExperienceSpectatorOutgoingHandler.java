package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;

public class SetExperienceSpectatorOutgoingHandler implements PacketHandler<ClientboundSetExperiencePacket, ServerConnection> {
    @Override
    public ClientboundSetExperiencePacket apply(ClientboundSetExperiencePacket packet, ServerConnection session) {
        return null;
    }
}
