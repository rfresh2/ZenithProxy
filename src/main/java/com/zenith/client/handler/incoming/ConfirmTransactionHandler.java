package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;


public class ConfirmTransactionHandler implements HandlerRegistry.AsyncIncomingHandler<ServerConfirmTransactionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerConfirmTransactionPacket packet, @NonNull ClientSession session) {
        // automatically accept Client-bound transactions from com/zenith/cache/data/PlayerCache.java:87
        // not doing this will fuck future inventory actions completely
        if (packet.getWindowId() == 0 && packet.getActionId() == -1337 && !packet.getAccepted()) {
            session.send(new ClientConfirmTransactionPacket(packet.getWindowId(), packet.getActionId(), true));
        }
        return true;
    }

    @Override
    public Class<ServerConfirmTransactionPacket> getPacketClass() {
        return ServerConfirmTransactionPacket.class;
    }
}
