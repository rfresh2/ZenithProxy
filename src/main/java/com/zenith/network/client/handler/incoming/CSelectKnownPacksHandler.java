package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import java.util.Collections;

public class CSelectKnownPacksHandler implements PacketHandler<ClientboundSelectKnownPacks, ClientSession> {
    @Override
    public ClientboundSelectKnownPacks apply(final ClientboundSelectKnownPacks packet, final ClientSession session) {
        session.sendAsync(new ServerboundSelectKnownPacks(Collections.emptyList()));
        return null;
    }
}
