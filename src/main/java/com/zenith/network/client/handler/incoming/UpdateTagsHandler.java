package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;

import static com.zenith.Shared.CACHE;

public class UpdateTagsHandler implements PacketHandler<ClientboundUpdateTagsPacket, ClientSession> {
    public static final UpdateTagsHandler INSTANCE = new UpdateTagsHandler();
    @Override
    public ClientboundUpdateTagsPacket apply(final ClientboundUpdateTagsPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().setTags(packet.getTags());
        return packet;
    }
}
