package com.zenith.network;

import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;

import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.EXECUTOR;

public class KeepAliveTask implements Runnable {
    private final ServerSession session;

    public KeepAliveTask(ServerSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        if (this.session.isConnected()) {
            session.setLastPingTime(System.currentTimeMillis());
            session.setLastPingId((int) session.getLastPingTime());
            var state = session.getPacketProtocol().getOutboundState();
            if (state == ProtocolState.CONFIGURATION || state == ProtocolState.GAME) {
                this.session.sendAsync(new ClientboundKeepAlivePacket(session.getLastPingId()));
            }
            EXECUTOR.schedule(this, 2L, TimeUnit.SECONDS);
        }
    }
}
