package com.zenith.network.client.handler.postoutgoing;

import com.zenith.cache.CacheResetType;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingConfigurationAckHandler implements PostOutgoingPacketHandler<ServerboundConfigurationAcknowledgedPacket, ClientSession> {
    @Override
    public void accept(final ServerboundConfigurationAcknowledgedPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
        CACHE.reset(CacheResetType.PROTOCOL_SWITCH);
    }
}
