package com.zenith.network;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;

@Data
public class ClientKeepAliveTask implements Runnable {
    private final ClientSession client;

    @Override
    public void run() {
        if (client.isTerminalState()) return;
        var protocolState = client.getProtocol().getOutboundState();
        if (protocolState != ProtocolState.CONFIGURATION && protocolState != ProtocolState.GAME) return;
        var keepAliveQueue = CACHE.getPlayerCache().getKeepAliveQueue();
        var keepAliveRequest = keepAliveQueue.peek();
        if (keepAliveRequest != null) {
            var elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - keepAliveRequest.receivedTime());
            // once a queue builds up we can't easily escape the backlog
            // on keep alive receive, the handler tries to respond immediately which will get blocked for being out of order
            // so at that point, this task is the only thing that can get us unstuck
            var timeout = Proxy.getInstance().hasActivePlayer()
                ? CONFIG.client.keepAliveHandling.keepAliveQueueTimeoutMs
                : 10; // try to get unstuck asap, can't be zero so we can account for normal responding time
            if (elapsed >= timeout) {
                keepAliveQueue.forEach(r -> {
                    CLIENT_LOG.debug("Sending timed out KeepAlive: {} queue size: {}", r, keepAliveQueue.size());
                    client.send(new ServerboundKeepAlivePacket(r.id()));
                });
            }
        }
    }
}
