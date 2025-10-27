package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.event.client.ClientConfigurationEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;

import static com.zenith.Globals.EVENT_BUS;

public class CStartConfigurationHandler implements PacketHandler<ClientboundStartConfigurationPacket, ClientSession> {
    @Override
    public ClientboundStartConfigurationPacket apply(final ClientboundStartConfigurationPacket packet, final ClientSession session) {
        EVENT_BUS.post(ClientConfigurationEvent.Entering.INSTANCE);
        session.setOnline(false);
        session.switchInboundState(ProtocolState.CONFIGURATION);
        if (!Proxy.getInstance().hasActivePlayer()) {
            session.send(new ServerboundConfigurationAcknowledgedPacket());
            return null;
        }
        return packet;
    }
}
