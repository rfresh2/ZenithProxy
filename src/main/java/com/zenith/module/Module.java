package com.zenith.module;

import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;

import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;

/**
 * Module system base class.
 */
public abstract class Module {

    public Module() {
        EVENT_BUS.subscribe(this);
    }

    public void clientTickStarting() {
    }

    public void clientTickStopping() {
    }

    public void sendClientPacketAsync(final Packet packet) {
        SCHEDULED_EXECUTOR_SERVICE.execute(() -> {
            ClientSession clientSession = Proxy.getInstance().getClient();
            if (clientSession != null && clientSession.isConnected()) {
                clientSession.send(packet);
            }
        });
    }
}
