package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundTransferPacket;

import static com.zenith.Shared.CLIENT_LOG;
import static com.zenith.Shared.EXECUTOR;

public class CTransferHandler implements PacketHandler<ClientboundTransferPacket, ClientSession> {
    @Override
    public ClientboundTransferPacket apply(final ClientboundTransferPacket packet, final ClientSession session) {
        var reason = Component.text("Destination server requested transfer to: " + packet.getHost() + ":" + packet.getPort());
        // todo: should we send a transfer packet to all active connections so they can reconnect to us seamlessly?
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.disconnect(reason);
        }
        session.disconnect(reason);
        // TODO: this does not follow the protocol exactly
        //  we need a persistent cookie store and we also need to connect with transfer intention
        //  but these should only matter in cases of server networks where they actually require cookies
        //  and knowing which connections are transfers or not (i.e. to block or allow them)
        EXECUTOR.execute(() -> {
            try {
                Proxy.getInstance().connect(packet.getHost(), packet.getPort());
            } catch (final Exception e) {
                CLIENT_LOG.error("Error connecting to transfer destination: {}:{}", packet.getHost(), packet.getPort(), e);
            }

        });
        return null;
    }
}
