package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

public class CommandsHandler implements IncomingHandler<ClientboundCommandsPacket, ClientSession> {
    @Override
    public boolean apply(final ClientboundCommandsPacket packet, final ClientSession session) {
        // todo: figure out why this is crashing the client when it switches game modes
        return false;
    }

    @Override
    public Class<ClientboundCommandsPacket> getPacketClass() {
        return ClientboundCommandsPacket.class;
    }
}
