package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundResourcePackPopPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import static com.zenith.Shared.CACHE;

public class ResourcePackPopHandler implements PacketHandler<ClientboundResourcePackPopPacket, ClientSession> {
    @Override
    public ClientboundResourcePackPopPacket apply(final ClientboundResourcePackPopPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().remove(packet.getId());
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
