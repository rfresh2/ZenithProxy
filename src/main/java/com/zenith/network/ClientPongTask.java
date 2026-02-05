package com.zenith.network;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;

@Data
public class ClientPongTask implements Runnable {
    private final ClientSession client;

    @Override
    public void run() {
        if (client.isTerminalState()) return;
        var protocolState = client.getProtocol().getOutboundState();
        if (protocolState != ProtocolState.CONFIGURATION && protocolState != ProtocolState.GAME) return;
        var pingQueue = CACHE.getPlayerCache().getPingQueue();
        var pingRequest = pingQueue.peek();
        if (pingRequest != null) {
            var elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pingRequest.receivedTime());
            // once a queue builds up we can't easily escape the backlog
            // on ping receive, the handler tries to respond immediately which will get blocked for being out of order
            // so at that point, this task is the only thing that can get us unstuck
            var timeout = Proxy.getInstance().hasActivePlayer()
                ? CONFIG.client.ping.pingQueueTimeoutMs
                : 50; // try to get unstuck asap, can't be zero so we can account for normal responding time
            if (elapsed >= timeout) {
                pingQueue.forEach(r -> {
                    CLIENT_LOG.debug("Sending timed out Pong: {} queue size: {}", r, pingQueue.size());
                    client.sendAsync(new ServerboundPongPacket(r.id()));
                });
            }
        }
    }
}
