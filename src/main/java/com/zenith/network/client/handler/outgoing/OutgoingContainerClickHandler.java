package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Shared.CACHE;

public class OutgoingContainerClickHandler implements PacketHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getActionId().set(packet.getStateId());
        return packet;
    }
}
