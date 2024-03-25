package com.zenith.network.client.handler.incoming.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;

public class ContainerOpenScreenHandler implements ClientEventLoopPacketHandler<ClientboundOpenScreenPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundOpenScreenPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().openContainer(packet.getContainerId(), packet.getType(), packet.getTitle());
        return true;
    }
}
