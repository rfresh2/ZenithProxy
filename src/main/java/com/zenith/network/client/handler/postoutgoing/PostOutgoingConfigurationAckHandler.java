package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

public class PostOutgoingConfigurationAckHandler implements PostOutgoingPacketHandler<ServerboundConfigurationAcknowledgedPacket, ClientSession> {
    @Override
    public void accept(final ServerboundConfigurationAcknowledgedPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
    }
}
