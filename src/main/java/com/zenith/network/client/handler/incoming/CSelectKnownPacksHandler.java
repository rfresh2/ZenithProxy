package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;

import java.util.Collections;

public class CSelectKnownPacksHandler implements PacketHandler<ClientboundSelectKnownPacks, ClientSession> {
    @Override
    public ClientboundSelectKnownPacks apply(final ClientboundSelectKnownPacks packet, final ClientSession session) {
        // we could also respond with the core pack instead. would prevent the server from sending all the registry data
//        session.sendAsync(new ServerboundSelectKnownPacks(List.of(new KnownPack("minecraft", "core", "1.21.0"))));
        session.send(new ServerboundSelectKnownPacks(Collections.emptyList()));
        return null;
    }
}
