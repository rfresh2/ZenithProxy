package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPopPacket;

import static com.zenith.Shared.CACHE;

public class ResourcePackPopHandler implements PacketHandler<ClientboundResourcePackPopPacket, ClientSession> {
    @Override
    public ClientboundResourcePackPopPacket apply(final ClientboundResourcePackPopPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().remove(packet.getId());
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
