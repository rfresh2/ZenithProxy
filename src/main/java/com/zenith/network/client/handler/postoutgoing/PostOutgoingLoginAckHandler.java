package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;

public class PostOutgoingLoginAckHandler implements PostOutgoingPacketHandler<ServerboundLoginAcknowledgedPacket, ClientSession> {
    @Override
    public void accept(final ServerboundLoginAcknowledgedPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
    }
}
