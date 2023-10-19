package com.zenith.network.client.handler.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.OutgoingHandler;

import static com.zenith.Shared.CACHE;

public class OutgoingContainerClickHandler implements OutgoingHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getActionId().set(packet.getStateId());
        return packet;
    }
}
