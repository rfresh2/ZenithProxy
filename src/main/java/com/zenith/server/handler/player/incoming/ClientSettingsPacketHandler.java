package com.zenith.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class ClientSettingsPacketHandler implements HandlerRegistry.AsyncIncomingHandler<ClientSettingsPacket, ServerConnection> {
    @Override
    public boolean applyAsync(ClientSettingsPacket packet, ServerConnection session) {
        CACHE.getChunkCache().setRenderDistance(packet.getRenderDistance());
        return true;
    }

    @Override
    public Class<ClientSettingsPacket> getPacketClass() {
        return ClientSettingsPacket.class;
    }
}
