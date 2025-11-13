package com.zenith.network;

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
            var elapsed = System.nanoTime() - keepAliveRequest.receivedTime();
            var timeout = TimeUnit.MILLISECONDS.toNanos(CONFIG.client.keepAliveHandling.keepAliveQueueTimeoutMs);
            if (elapsed >= timeout) {
                CLIENT_LOG.debug("Sending timed out KeepAlive: {} queue size: {}", keepAliveQueue, keepAliveQueue.size());
                client.send(new ServerboundKeepAlivePacket(keepAliveRequest.id()));
            }
        }
    }
}
