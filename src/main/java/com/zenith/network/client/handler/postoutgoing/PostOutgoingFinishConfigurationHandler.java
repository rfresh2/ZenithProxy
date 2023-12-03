package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

public class PostOutgoingFinishConfigurationHandler implements PostOutgoingPacketHandler<ServerboundFinishConfigurationPacket, ClientSession> {
    @Override
    public void accept(final ServerboundFinishConfigurationPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.GAME); // CONFIGURATION -> GAME
    }
}
