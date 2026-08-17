package com.zenith.network;

import com.zenith.network.server.ServerSession;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import static com.zenith.Globals.CONFIG;

/**
 * Disconnect players connecting to zenith if they take too long to handshake, server list ping, or login
 */
@Data
public class STimeoutTask implements Runnable {
    private final ServerSession session;
    // timer starts now, at task creation time
    private final Timer timer = Timers.unsyncedTickTimer();

    @Override
    public void run() {
        if (session.getChannel() == null || !session.getChannel().isActive()) {
            throw new RuntimeException("Task completed");
        }
        if (session.getLoginState() == ServerSession.LoginState.ACCEPTED) {
            throw new RuntimeException("Task completed");
        }
        if (!CONFIG.server.loginTimeout.enabled) return;
        if (session.getPacketProtocol().getInboundState() == ProtocolState.LOGIN) {
            if (timer.tick(CONFIG.server.loginTimeout.loginTimeoutTicks, false)) {
                session.disconnect("Login timed out");
            }
        } else if (session.getPacketProtocol().getInboundState() == ProtocolState.STATUS) {
            if (timer.tick(CONFIG.server.loginTimeout.statusTimeoutTicks, false)) {
                session.disconnect("Status request timed out");
            }
        } else if (session.getPacketProtocol().getInboundState() == ProtocolState.HANDSHAKE) {
            if (timer.tick(CONFIG.server.loginTimeout.handshakeTimeoutTicks, false)) {
                session.disconnect("Handshake timed out");
            }
        }
    }
}
