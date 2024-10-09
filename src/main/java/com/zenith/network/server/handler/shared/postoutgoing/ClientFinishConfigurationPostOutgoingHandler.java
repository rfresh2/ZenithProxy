package com.zenith.network.server.handler.shared.postoutgoing;

import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;

public class ClientFinishConfigurationPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundFinishConfigurationPacket, ServerSession> {
    @Override
    public void accept(final ClientboundFinishConfigurationPacket packet, final ServerSession session) {
        session.getPacketProtocol().setOutboundState(ProtocolState.GAME);
    }
}
