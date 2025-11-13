package com.zenith.network;

import com.zenith.network.client.ClientSession;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundPingRequestPacket;

/**
 * Zenith client -> server ping task
 */
public class ClientPacketPingTask implements Runnable {
    private final ClientSession session;

    public ClientPacketPingTask(final ClientSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        if (session.isTerminalState()) return;
        if (session.getPacketProtocol().getOutboundState() != ProtocolState.GAME) return;
        var id = System.currentTimeMillis();
        try {
            session.send(new ServerboundPingRequestPacket(id), f -> {
                session.setLastPingId(id);
                session.setLastPingSentTime(System.currentTimeMillis());
            });
        } catch (final Throwable e) {
            // fall through
        }
    }
}
