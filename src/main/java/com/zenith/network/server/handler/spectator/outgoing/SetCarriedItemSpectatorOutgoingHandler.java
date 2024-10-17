package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHeldSlotPacket;

public class SetCarriedItemSpectatorOutgoingHandler implements PacketHandler<ClientboundSetHeldSlotPacket, ServerSession> {
    @Override
    public ClientboundSetHeldSlotPacket apply(ClientboundSetHeldSlotPacket packet, ServerSession session) {
        return null;
    }
}
