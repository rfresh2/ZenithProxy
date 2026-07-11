package com.zenith.network;

import com.zenith.network.server.ServerSession;
import com.zenith.util.config.Config.Client.KeepAliveHandling.KeepAliveMode;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;

import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.SERVER_LOG;

public class SKeepAliveTask implements Runnable {
    private final ServerSession session;

    public SKeepAliveTask(ServerSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        var state = session.getPacketProtocol().getOutboundState();
        if (state != ProtocolState.CONFIGURATION && state != ProtocolState.GAME) return;
        // detect half-open connections that are no longer responding to keep alives
        // e.g. a controlling player whose internet connection has silently dropped
        // disconnecting them lets the bot take over and resync teleports before the destination server kicks us
        if (isKeepAliveTimedOut()) {
            SERVER_LOG.info("[{}] Disconnecting session that has not responded to keep alives", session.getName());
            session.disconnect("Timed out");
            return;
        }
        if (!session.isSpectator() && CONFIG.client.keepAliveHandling.keepAliveMode != KeepAliveMode.INDEPENDENT) return;
        this.session.send(new ClientboundKeepAlivePacket(System.currentTimeMillis()));
    }

    private boolean isKeepAliveTimedOut() {
        if (!CONFIG.server.extra.timeout.enable) return false;
        if (!session.isLoggedIn()) return false;
        var elapsed = System.currentTimeMillis() - session.getLastKeepAliveResponseTime();
        return elapsed > TimeUnit.SECONDS.toMillis(CONFIG.server.extra.timeout.seconds);
    }
}
