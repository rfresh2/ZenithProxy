package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;

public class SetHealthSpectatorOutgoingHandler implements PacketHandler<ClientboundSetHealthPacket, ServerSession> {
    @Override
    public ClientboundSetHealthPacket apply(ClientboundSetHealthPacket packet, ServerSession session) {
        return null;
    }
}
