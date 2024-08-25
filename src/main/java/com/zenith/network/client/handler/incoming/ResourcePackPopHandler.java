package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPopPacket;

import static com.zenith.Shared.CACHE;

public class ResourcePackPopHandler implements PacketHandler<ClientboundResourcePackPopPacket, ClientSession> {
    public static final ResourcePackPopHandler INSTANCE = new ResourcePackPopHandler();
    @Override
    public ClientboundResourcePackPopPacket apply(final ClientboundResourcePackPopPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().remove(packet.getId());
        return packet;
    }
}
