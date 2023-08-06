package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.util.Wait;
import lombok.NonNull;


public class ConfirmTransactionHandler implements AsyncIncomingHandler<ServerConfirmTransactionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerConfirmTransactionPacket packet, @NonNull ClientSession session) {
        // automatically accept Client-bound transactions from com/zenith/cache/data/PlayerCache.java:87
        // not doing this will fuck future inventory actions completely
        if (packet.getWindowId() == 0 && packet.getActionId() == -1337 && !packet.getAccepted()) {
            session.send(new ClientConfirmTransactionPacket(packet.getWindowId(), packet.getActionId(), true));
        }
        // Via Ping packet compat
        if (Proxy.getInstance().getCurrentPlayer().get() == null) {
            Wait.waitALittleMs(300);
            session.send(new ClientConfirmTransactionPacket(packet.getWindowId(), packet.getActionId(), true));
        }
        return true;
    }

    @Override
    public Class<ServerConfirmTransactionPacket> getPacketClass() {
        return ServerConfirmTransactionPacket.class;
    }
}
