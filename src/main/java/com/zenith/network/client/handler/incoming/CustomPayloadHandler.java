package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

public class CustomPayloadHandler implements IncomingHandler<ClientboundCustomPayloadPacket, ClientSession> {
    @Override
    public boolean apply(ClientboundCustomPayloadPacket packet, ClientSession session) {
        return !packet.getChannel().equalsIgnoreCase("minecraft:brand");
    }
}
