package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import static com.zenith.Shared.CACHE;

public class CRegistryDataHandler implements PacketHandler<ClientboundRegistryDataPacket, ClientSession> {
    @Override
    public ClientboundRegistryDataPacket apply(final ClientboundRegistryDataPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getRegistryEntries().put(packet.getRegistry(), packet.getEntries());
        if ("minecraft:dimension_type".equals(packet.getRegistry())) {
            // TODO: test
//            CACHE.getChunkCache().updateRegistryTag(packet.getEntries());
        }
        return packet;
    }
}
