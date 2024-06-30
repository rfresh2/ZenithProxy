package com.zenith.network.server.handler.shared.incoming;

import com.zenith.Proxy;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;

import static com.zenith.Shared.CACHE;

public class LoginAckHandler implements PacketHandler<ServerboundLoginAcknowledgedPacket, ServerSession> {
    @Override
    public ServerboundLoginAcknowledgedPacket apply(final ServerboundLoginAcknowledgedPacket packet, final ServerSession session) {
        session.getPacketProtocol().setState(ProtocolState.CONFIGURATION);
        // todo: handle this more gracefully, connect and wait until we have configuration set (assuming session is auth'd)
        if (!Proxy.getInstance().isConnected()) {
            session.disconnect("Proxy is not connected to a server.");
            return null;
        }
        CACHE.getConfigurationCache().getPackets(session::sendAsync);
        session.sendAsync(new ClientboundCustomPayloadPacket(Key.key("minecraft:brand"), CACHE.getChunkCache().getServerBrand()));
        session.sendAsync(new ClientboundFinishConfigurationPacket());
        return null;
    }
}
