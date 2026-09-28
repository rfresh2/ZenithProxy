package com.zenith.network.client.handler.outgoing;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.CACHE;

public class OutgoingContainerClickHandler implements PacketHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ClientSession session) {
        if (Proxy.getInstance().hasActivePlayer()) {
            CACHE.getPlayerCache().getInventoryCache().getOpenContainer().setStateId(packet.getStateId());
        }
        return packet;
    }
}
