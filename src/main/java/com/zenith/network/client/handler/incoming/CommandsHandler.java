package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

import static com.zenith.Shared.CACHE;

public class CommandsHandler implements IncomingHandler<ClientboundCommandsPacket, ClientSession> {
    @Override
    public boolean apply(final ClientboundCommandsPacket packet, final ClientSession session) {
        CACHE.getChatCache().setCommandNodes(packet.getNodes());
        CACHE.getChatCache().setFirstCommandNodeIndex(packet.getFirstNodeIndex());
        return true;
    }

    @Override
    public Class<ClientboundCommandsPacket> getPacketClass() {
        return ClientboundCommandsPacket.class;
    }
}
