package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import static com.zenith.Shared.CACHE;

public class UpdateTagsHandler implements PacketHandler<ClientboundUpdateTagsPacket, ClientSession> {
    public static UpdateTagsHandler INSTANCE = new UpdateTagsHandler();
    @Override
    public ClientboundUpdateTagsPacket apply(final ClientboundUpdateTagsPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().setTags(packet.getTags());
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
