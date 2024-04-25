package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;

public class CFinishConfigurationHandler implements PacketHandler<ClientboundFinishConfigurationPacket, ClientSession> {
    @Override
    public ClientboundFinishConfigurationPacket apply(final ClientboundFinishConfigurationPacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer()) {
            session.send(new ServerboundFinishConfigurationPacket());
            return null;
        }
        return packet;
    }
}
