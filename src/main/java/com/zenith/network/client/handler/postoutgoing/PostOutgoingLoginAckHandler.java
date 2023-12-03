package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

public class PostOutgoingLoginAckHandler implements PostOutgoingPacketHandler<ServerboundLoginAcknowledgedPacket, ClientSession> {
    @Override
    public void accept(final ServerboundLoginAcknowledgedPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
    }
}
