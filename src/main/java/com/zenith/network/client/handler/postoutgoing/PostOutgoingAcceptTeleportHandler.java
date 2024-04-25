package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

import static com.zenith.Shared.CACHE;

public class PostOutgoingAcceptTeleportHandler implements PostOutgoingPacketHandler<ServerboundAcceptTeleportationPacket, ClientSession> {

    @Override
    public void accept(final ServerboundAcceptTeleportationPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setLastTeleportAccepted(packet.getId());
    }
}
