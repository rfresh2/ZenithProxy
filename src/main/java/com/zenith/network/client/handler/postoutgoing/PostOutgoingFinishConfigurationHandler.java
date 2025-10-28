package com.zenith.network.client.handler.postoutgoing;

import com.zenith.event.client.ClientConfigurationEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;

import static com.zenith.Globals.EVENT_BUS;

public class PostOutgoingFinishConfigurationHandler implements PostOutgoingPacketHandler<ServerboundFinishConfigurationPacket, ClientSession> {
    @Override
    public void accept(final ServerboundFinishConfigurationPacket packet, final ClientSession session) {
        session.getPacketProtocol().setOutboundState(ProtocolState.GAME); // CONFIGURATION -> GAME
        EVENT_BUS.post(ClientConfigurationEvent.Exited.INSTANCE);
    }
}
