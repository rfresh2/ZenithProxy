package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;

public class PostOutgoingFinishConfigurationHandler implements PostOutgoingPacketHandler<ServerboundFinishConfigurationPacket, ClientSession> {
    @Override
    public void accept(final ServerboundFinishConfigurationPacket packet, final ClientSession session) {
        session.getPacketProtocol().setOutboundState(ProtocolState.GAME); // CONFIGURATION -> GAME
    }
}
