package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;

public class SetExperienceSpectatorOutgoingHandler implements PacketHandler<ClientboundSetExperiencePacket, ServerSession> {
    @Override
    public ClientboundSetExperiencePacket apply(ClientboundSetExperiencePacket packet, ServerSession session) {
        return null;
    }
}
