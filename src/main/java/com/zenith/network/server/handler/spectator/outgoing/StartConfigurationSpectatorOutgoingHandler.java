package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class StartConfigurationSpectatorOutgoingHandler implements PacketHandler<ClientboundStartConfigurationPacket, ServerSession> {
    @Override
    public ClientboundStartConfigurationPacket apply(final ClientboundStartConfigurationPacket packet, final ServerSession session) {
        if (session.isConfigured()) {
            if (session.canTransfer()) {
                SERVER_LOG.info("Reconnecting spectator: {} because client is switching servers", session.getProfileCache().getProfile().getName());
                session.transferToSpectator(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            } else {
                session.disconnect("Client is switching servers");
            }
            return null;
        }
        return packet;
    }
}
