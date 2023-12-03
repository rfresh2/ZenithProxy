package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.zenith.Proxy;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class LoginAckHandler implements PacketHandler<ServerboundLoginAcknowledgedPacket, ServerConnection> {
    @Override
    public ServerboundLoginAcknowledgedPacket apply(final ServerboundLoginAcknowledgedPacket packet, final ServerConnection session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION);
        // todo: handle this more gracefully, connect and wait until we have configuration set (assuming session is auth'd)
        if (!Proxy.getInstance().isConnected()) {
            session.disconnect("Proxy is not connected to a server.");
            return null;
        }
        CACHE.getConfigurationCache().getPackets(session::sendAsync);
        session.sendAsync(new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()));
        session.sendAsync(new ClientboundFinishConfigurationPacket());
        return null;
    }
}
