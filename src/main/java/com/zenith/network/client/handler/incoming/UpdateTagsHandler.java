package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import static com.zenith.Shared.CACHE;

public class UpdateTagsHandler implements PacketHandler<ClientboundUpdateTagsPacket, ClientSession> {
    @Override
    public ClientboundUpdateTagsPacket apply(final ClientboundUpdateTagsPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().setTags(packet.getTags());
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
