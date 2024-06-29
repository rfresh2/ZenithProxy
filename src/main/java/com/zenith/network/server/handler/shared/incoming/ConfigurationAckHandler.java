package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;

public class ConfigurationAckHandler implements PacketHandler<ServerboundConfigurationAcknowledgedPacket, ServerConnection> {
    @Override
    public ServerboundConfigurationAcknowledgedPacket apply(final ServerboundConfigurationAcknowledgedPacket packet, final ServerConnection session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION);
        session.setAwaitingProtocolSwitch(false);
        return packet;
    }
}
